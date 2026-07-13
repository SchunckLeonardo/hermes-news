package com.hermesnews.watchlist;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("app.watchlist")
public record WatchlistProperties(
		Duration defaultCooldown,
		Duration maxArticleAge,
		int minScore,
		int maxAlertsPerRun) {

	@ConstructorBinding
	public WatchlistProperties {
		defaultCooldown = defaultCooldown == null || defaultCooldown.isNegative() || defaultCooldown.isZero()
				? Duration.ofHours(6)
				: defaultCooldown;
		maxArticleAge = maxArticleAge == null || maxArticleAge.isNegative() || maxArticleAge.isZero()
				? Duration.ofHours(24)
				: maxArticleAge;
		maxAlertsPerRun = Math.max(1, maxAlertsPerRun);
	}
}
