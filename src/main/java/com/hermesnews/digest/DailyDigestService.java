package com.hermesnews.digest;

import com.hermesnews.ai.AiSummaryService;
import com.hermesnews.news.Article;
import com.hermesnews.news.ArticleRepository;
import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsCollector;
import com.hermesnews.observability.HermesMetrics;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.ranking.RankedArticle;
import com.hermesnews.ranking.SemanticEventClusterer;
import com.hermesnews.ranking.RankingService;
import com.hermesnews.whatsapp.WhatsAppService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DailyDigestService {

	private static final Logger log = LoggerFactory.getLogger(DailyDigestService.class);

	private final List<NewsCollector> collectors;
	private final ArticleRepository articleRepository;
	private final DigestRepository digestRepository;
	private final DigestItemRepository digestItemRepository;
	private final RankingService rankingService;
	private final AiSummaryService aiSummaryService;
	private final WhatsAppService whatsAppService;
	private final PreferenceService preferenceService;
	private final SemanticEventClusterer eventClusterer;
	private final HermesMetrics metrics;

	@Autowired
	public DailyDigestService(
			List<NewsCollector> collectors,
			ArticleRepository articleRepository,
			DigestRepository digestRepository,
			DigestItemRepository digestItemRepository,
			RankingService rankingService,
			AiSummaryService aiSummaryService,
			WhatsAppService whatsAppService,
			PreferenceService preferenceService,
			SemanticEventClusterer eventClusterer,
			HermesMetrics metrics) {
		this.collectors = collectors;
		this.articleRepository = articleRepository;
		this.digestRepository = digestRepository;
		this.digestItemRepository = digestItemRepository;
		this.rankingService = rankingService;
		this.aiSummaryService = aiSummaryService;
		this.whatsAppService = whatsAppService;
		this.preferenceService = preferenceService;
		this.eventClusterer = eventClusterer;
		this.metrics = metrics;
	}

	public DailyDigestService(
			List<NewsCollector> collectors,
			ArticleRepository articleRepository,
			DigestRepository digestRepository,
			DigestItemRepository digestItemRepository,
			RankingService rankingService,
			AiSummaryService aiSummaryService,
			WhatsAppService whatsAppService,
			PreferenceService preferenceService) {
		this(
				collectors,
				articleRepository,
				digestRepository,
				digestItemRepository,
				rankingService,
				aiSummaryService,
				whatsAppService,
				preferenceService,
				new SemanticEventClusterer(),
				HermesMetrics.noop());
	}

	@Transactional
	public DailyDigestResult sendDailyDigest() {
		var collected = collectAll();
		var newArticles = deduplicateByUrl(collected).stream()
				.filter(article -> !articleRepository.existsByUrl(article.url()))
				.toList();
		var newsLimit = Math.max(1, preferenceService.current().newsLimit());
		var ranked = eventClusterer.cluster(rankingService.rank(newArticles)).stream()
				.limit(newsLimit)
				.toList();
		var savedArticles = new ArrayList<Article>();
		for (RankedArticle rankedArticle : ranked) {
			savedArticles.add(articleRepository.save(Article.from(rankedArticle.article(), rankedArticle.score())));
		}

		var message = aiSummaryService.summarize(ranked);
		var digest = digestRepository.save(Digest.created(message));
		for (int index = 0; index < savedArticles.size(); index++) {
			var rankedArticle = ranked.get(index);
			digestItemRepository.save(new DigestItem(
					digest,
					savedArticles.get(index),
					rankedArticle.score(),
					index + 1,
					rankedArticle.explanation(),
					rankedArticle.eventKey()));
		}
		var sendResult = whatsAppService.sendText(message);
		digest.applySendStatus(sendResult.status());
		digestRepository.save(digest);
		metrics.recordDigest(collected.size(), savedArticles.size(), sendResult.status());
		return new DailyDigestResult(savedArticles.size(), message, sendResult.status());
	}

	private List<CollectedArticle> collectAll() {
		var articles = new ArrayList<CollectedArticle>();
		for (NewsCollector collector : collectors) {
			try {
				articles.addAll(collector.collect());
			}
			catch (RuntimeException exception) {
				log.warn("Collector {} failed: {}", collector.getClass().getSimpleName(), exception.getMessage());
			}
		}
		return articles.stream()
				.filter(article -> article.url() != null && !article.url().isBlank())
				.filter(article -> article.title() != null && !article.title().isBlank())
				.toList();
	}

	private static List<CollectedArticle> deduplicateByUrl(List<CollectedArticle> articles) {
		var byUrl = new LinkedHashMap<String, CollectedArticle>();
		for (CollectedArticle article : articles) {
			byUrl.putIfAbsent(article.url(), article);
		}
		return List.copyOf(byUrl.values());
	}
}
