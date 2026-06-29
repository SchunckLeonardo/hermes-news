package com.hermesnews.ranking;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ranking")
public record RankingProperties(List<String> keywords) {
}
