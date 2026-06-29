package com.hermesnews.news;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rss")
public record RssProperties(List<String> feeds) {
}
