package com.hermesnews.news;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hacker-news")
public record HackerNewsProperties(String baseUrl, int maxItems) {
}
