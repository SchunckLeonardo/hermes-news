package com.hermesnews.watchlist;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsCollector;
import com.hermesnews.ranking.RankedArticle;
import com.hermesnews.ranking.RankingService;
import com.hermesnews.ranking.SemanticEventClusterer;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import com.hermesnews.whatsapp.WhatsAppService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

	private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

	private final List<NewsCollector> collectors;
	private final RankingService rankingService;
	private final SemanticEventClusterer eventClusterer;
	private final WatchlistEntryRepository entryRepository;
	private final UrgentAlertRepository alertRepository;
	private final WhatsAppService whatsAppService;
	private final Clock clock;
	private final WatchlistProperties properties;

	public WatchlistService(
			List<NewsCollector> collectors,
			RankingService rankingService,
			SemanticEventClusterer eventClusterer,
			WatchlistEntryRepository entryRepository,
			UrgentAlertRepository alertRepository,
			WhatsAppService whatsAppService,
			Clock clock,
			WatchlistProperties properties) {
		this.collectors = collectors;
		this.rankingService = rankingService;
		this.eventClusterer = eventClusterer;
		this.entryRepository = entryRepository;
		this.alertRepository = alertRepository;
		this.whatsAppService = whatsAppService;
		this.clock = clock;
		this.properties = properties;
	}

	@Transactional
	public WatchlistEntry add(String term) {
		var requested = term == null ? "" : term.trim();
		return entryRepository.findByTermIgnoreCase(requested)
				.map(entry -> {
					entry.enable();
					return entryRepository.save(entry);
				})
				.orElseGet(() -> entryRepository.save(new WatchlistEntry(requested, properties.defaultCooldown())));
	}

	@Transactional
	public WatchlistEntry remove(String term) {
		var entry = entryRepository.findByTermIgnoreCase(term == null ? "" : term.trim())
				.orElseThrow(() -> new IllegalArgumentException("Watchlist term was not found"));
		entry.disable();
		return entryRepository.save(entry);
	}

	@Transactional
	public List<WatchlistEntry> activeEntries() {
		return entryRepository.findAllByEnabledTrueOrderByTermAsc();
	}

	@Transactional
	public UrgentScanResult scanAndAlert() {
		var now = clock.instant();
		var availableEntries = entryRepository.findAllByEnabledTrueOrderByTermAsc().stream()
				.filter(entry -> entry.canAlert(now))
				.toList();
		if (availableEntries.isEmpty()) {
			return new UrgentScanResult(0, 0);
		}
		var collected = collectAll().stream()
				.filter(article -> isFresh(article, now))
				.filter(article -> !alertRepository.existsByArticleUrl(article.url()))
				.toList();
		var candidates = eventClusterer.cluster(rankingService.rank(deduplicateByUrl(collected))).stream()
				.filter(ranked -> ranked.score() >= properties.minScore())
				.toList();
		var alerts = 0;
		for (RankedArticle ranked : candidates) {
			var entry = matchingEntry(availableEntries, ranked.article());
			if (entry == null || !entry.canAlert(now)) {
				continue;
			}
			var result = whatsAppService.sendText(alertMessage(entry, ranked));
			if (result.status() == WhatsAppSendStatus.SKIPPED) {
				continue;
			}
			alertRepository.save(new UrgentAlert(
					entry,
					ranked.article().url(),
					ranked.article().title(),
					now));
			entry.markAlerted(now);
			entryRepository.save(entry);
			alerts++;
			if (alerts >= properties.maxAlertsPerRun()) {
				break;
			}
		}
		return new UrgentScanResult(candidates.size(), alerts);
	}

	private List<CollectedArticle> collectAll() {
		var articles = new ArrayList<CollectedArticle>();
		for (NewsCollector collector : collectors) {
			try {
				articles.addAll(collector.collect());
			}
			catch (RuntimeException exception) {
				log.warn("Watchlist collector {} failed: {}", collector.getClass().getSimpleName(), exception.getMessage());
			}
		}
		return articles;
	}

	private boolean isFresh(CollectedArticle article, Instant now) {
		return article.url() != null
				&& !article.url().isBlank()
				&& article.title() != null
				&& !article.title().isBlank()
				&& article.publishedAt() != null
				&& !article.publishedAt().isBefore(now.minus(properties.maxArticleAge()))
				&& !article.publishedAt().isAfter(now.plusSeconds(300));
	}

	private static List<CollectedArticle> deduplicateByUrl(List<CollectedArticle> articles) {
		var byUrl = new LinkedHashMap<String, CollectedArticle>();
		for (CollectedArticle article : articles) {
			byUrl.putIfAbsent(article.url(), article);
		}
		return List.copyOf(byUrl.values());
	}

	private static WatchlistEntry matchingEntry(List<WatchlistEntry> entries, CollectedArticle article) {
		var text = normalize(article.title() + " " + article.summary());
		return entries.stream().filter(entry -> containsTerm(text, entry.getTerm())).findFirst().orElse(null);
	}

	private static boolean containsTerm(String text, String term) {
		var normalizedTerm = normalize(term);
		if (normalizedTerm.length() <= 3 && normalizedTerm.chars().allMatch(Character::isLetterOrDigit)) {
			return text.matches(".*\\b" + java.util.regex.Pattern.quote(normalizedTerm) + "\\b.*");
		}
		return text.contains(normalizedTerm);
	}

	private static String alertMessage(WatchlistEntry entry, RankedArticle ranked) {
		var article = ranked.article();
		return """
				*Hermes Alerta*
				Monitoramento: %s

				*%s*
				Fonte: %s
				Link: %s
				""".formatted(entry.getTerm(), article.title(), article.sourceName(), article.url()).trim();
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
