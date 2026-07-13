package com.hermesnews.history;

import com.hermesnews.news.ArticleRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsHistoryService {

	private final ArticleRepository articleRepository;
	private final Clock clock;

	public NewsHistoryService(ArticleRepository articleRepository, Clock clock) {
		this.articleRepository = articleRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public HistorySearchResult search(String query, Duration period, int limit) {
		var requested = query == null ? "" : query.trim();
		if (requested.isBlank() || requested.length() > 160) {
			throw new IllegalArgumentException("History query must have between 1 and 160 characters");
		}
		var safePeriod = period == null || period.isNegative() || period.isZero() ? Duration.ofDays(7) : period;
		var since = clock.instant().minus(safePeriod);
		var safeLimit = Math.max(1, Math.min(10, limit));
		var articles = articleRepository.searchHistory(
				requested.toLowerCase(Locale.ROOT),
				since,
				PageRequest.of(0, safeLimit));
		return new HistorySearchResult(requested, since, articles);
	}
}
