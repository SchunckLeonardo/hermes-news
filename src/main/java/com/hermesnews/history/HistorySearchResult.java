package com.hermesnews.history;

import com.hermesnews.news.Article;
import java.time.Instant;
import java.util.List;

public record HistorySearchResult(String query, Instant since, List<Article> articles) {

	public HistorySearchResult {
		articles = articles == null ? List.of() : List.copyOf(articles);
	}
}
