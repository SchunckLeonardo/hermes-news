package com.hermesnews.news;

public record NewsSourceTestResponse(
		String url,
		boolean success,
		int articleCount,
		String resolvedFeedUrl,
		String message) {
}
