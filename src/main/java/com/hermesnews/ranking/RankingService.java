package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.preferences.PreferenceService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

	private static final int OFFICIAL_SOURCE_BONUS = 12;
	private static final int PRIORITY_TITLE_BONUS = 8;
	private static final int PRIORITY_SUMMARY_BONUS = 3;
	private static final int LAUNCH_TITLE_BONUS = 7;
	private static final int LAUNCH_SUMMARY_BONUS = 3;
	private static final int RECENT_24H_BONUS = 8;
	private static final int RECENT_72H_BONUS = 4;
	private static final int RECENT_7D_BONUS = 2;

	private final List<String> keywords;
	private final List<String> officialSources;
	private final List<String> priorityEntities;
	private final List<String> launchKeywords;
	private final PreferenceService preferenceService;
	private final Clock clock;
	private final RankingFeedbackProvider feedbackProvider;

	public RankingService(RankingProperties properties) {
		this(properties, null, Clock.systemUTC(), article -> FeedbackAdjustment.none());
	}

	@Autowired
	public RankingService(
			RankingProperties properties,
			PreferenceService preferenceService,
			Clock clock,
			RankingFeedbackProvider feedbackProvider) {
		this.keywords = normalizeList(properties.keywords());
		this.officialSources = normalizeList(properties.officialSources());
		this.priorityEntities = normalizeList(properties.priorityEntities());
		this.launchKeywords = normalizeList(properties.launchKeywords());
		this.preferenceService = preferenceService;
		this.clock = clock == null ? Clock.systemUTC() : clock;
		this.feedbackProvider = feedbackProvider == null ? article -> FeedbackAdjustment.none() : feedbackProvider;
	}

	public RankingService(RankingProperties properties, PreferenceService preferenceService) {
		this(properties, preferenceService, Clock.systemUTC(), article -> FeedbackAdjustment.none());
	}

	public RankingService(RankingProperties properties, PreferenceService preferenceService, Clock clock) {
		this(properties, preferenceService, clock, article -> FeedbackAdjustment.none());
	}

	public int score(CollectedArticle article) {
		return evaluate(article).score();
	}

	private RankedArticle evaluate(CollectedArticle article) {
		var title = normalize(article.title());
		var summary = normalize(article.summary());
		var source = normalize(article.sourceName());
		var url = normalize(article.url());
		var preferences = preferenceService == null ? null : preferenceService.current();
		var preferredThemes = preferences == null ? List.<String>of() : preferences.themes();
		var excludedThemes = preferences == null ? Set.<String>of() : Set.copyOf(preferences.excludedThemes());
		var preferredSources = preferences == null ? List.<String>of() : preferences.sources();
		var reasons = new java.util.ArrayList<RankingReason>();
		var score = 0;
		var keywordScore = 0;
		for (String keyword : keywords) {
			if (containsSignal(title, keyword)) {
				keywordScore += 3;
			}
			if (containsSignal(summary, keyword)) {
				keywordScore += 1;
			}
		}
		score += addReason(reasons, "keywords", keywordScore, "Palavras-chave relevantes");
		if (matchesAny(source + " " + url, officialSources)) {
			score += addReason(reasons, "official-source", OFFICIAL_SOURCE_BONUS, "Fonte oficial");
		}
		var priorityScore = 0;
		for (String entity : priorityEntities) {
			if (containsSignal(title, entity)) {
				priorityScore += PRIORITY_TITLE_BONUS;
			}
			if (containsSignal(summary, entity)) {
				priorityScore += PRIORITY_SUMMARY_BONUS;
			}
		}
		score += addReason(reasons, "priority-entity", priorityScore, "Entidades prioritarias");
		var launchScore = 0;
		for (String launchKeyword : launchKeywords) {
			if (containsSignal(title, launchKeyword)) {
				launchScore += LAUNCH_TITLE_BONUS;
			}
			if (containsSignal(summary, launchKeyword)) {
				launchScore += LAUNCH_SUMMARY_BONUS;
			}
		}
		score += addReason(reasons, "launch", launchScore, "Sinal de anuncio ou lancamento");
		var recency = recencyBonus(article.publishedAt());
		var recencyDescription = switch (recency) {
			case RECENT_24H_BONUS -> "Publicada nas ultimas 24 horas";
			case RECENT_72H_BONUS -> "Publicada nas ultimas 72 horas";
			case RECENT_7D_BONUS -> "Publicada nos ultimos 7 dias";
			default -> "Recencia";
		};
		score += addReason(reasons, "recency", recency, recencyDescription);
		var preferredThemeScore = 0;
		for (String theme : preferredThemes) {
			if (title.contains(theme)) {
				preferredThemeScore += 5;
			}
			if (summary.contains(theme)) {
				preferredThemeScore += 2;
			}
		}
		score += addReason(reasons, "preferred-theme", preferredThemeScore, "Temas preferidos");
		var excludedThemeScore = 0;
		for (String excluded : excludedThemes) {
			if (title.contains(excluded)) {
				excludedThemeScore -= 5;
			}
			if (summary.contains(excluded)) {
				excludedThemeScore -= 2;
			}
		}
		score += addReason(reasons, "excluded-theme", excludedThemeScore, "Temas com menor prioridade");
		var preferredSourceScore = 0;
		for (String preferredSource : preferredSources) {
			if (source.contains(preferredSource)) {
				preferredSourceScore += 4;
			}
		}
		score += addReason(reasons, "preferred-source", preferredSourceScore, "Fonte preferida");
		var feedback = feedbackProvider.adjustmentFor(article);
		if (feedback != null) {
			score += addReason(reasons, "feedback", feedback.points(), feedback.reason());
		}
		return new RankedArticle(article, score, reasons);
	}

	public List<RankedArticle> rank(List<CollectedArticle> articles) {
		return articles.stream()
				.map(this::evaluate)
				.sorted(Comparator.comparingInt(RankedArticle::score).reversed()
						.thenComparing(ranked -> ranked.article().publishedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private static int addReason(List<RankingReason> reasons, String code, int points, String description) {
		if (points != 0 && description != null && !description.isBlank()) {
			reasons.add(new RankingReason(code, points, description));
		}
		return points;
	}

	private int recencyBonus(Instant publishedAt) {
		if (publishedAt == null) {
			return 0;
		}
		var age = Duration.between(publishedAt, Instant.now(clock));
		if (age.isNegative()) {
			return RECENT_24H_BONUS;
		}
		if (age.compareTo(Duration.ofHours(24)) <= 0) {
			return RECENT_24H_BONUS;
		}
		if (age.compareTo(Duration.ofHours(72)) <= 0) {
			return RECENT_72H_BONUS;
		}
		if (age.compareTo(Duration.ofDays(7)) <= 0) {
			return RECENT_7D_BONUS;
		}
		return 0;
	}

	private static List<String> normalizeList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(RankingService::normalize)
				.toList();
	}

	private static boolean matchesAny(String text, List<String> signals) {
		for (String signal : signals) {
			if (containsSignal(text, signal)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsSignal(String text, String signal) {
		if (text == null || text.isBlank() || signal == null || signal.isBlank()) {
			return false;
		}
		if (signal.length() <= 3 && signal.chars().allMatch(Character::isLetterOrDigit)) {
			return text.matches(".*\\b" + java.util.regex.Pattern.quote(signal) + "\\b.*");
		}
		return text.contains(signal);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
