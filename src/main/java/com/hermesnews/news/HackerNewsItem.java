package com.hermesnews.news;

public record HackerNewsItem(
		Long id,
		String title,
		String url,
		Long time,
		Boolean dead,
		Boolean deleted) {
}
