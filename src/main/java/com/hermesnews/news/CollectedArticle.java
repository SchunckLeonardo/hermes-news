package com.hermesnews.news;

import java.time.Instant;

public record CollectedArticle(
		String sourceName,
		String externalId,
		String title,
		String url,
		String summary,
		Instant publishedAt) {
}
