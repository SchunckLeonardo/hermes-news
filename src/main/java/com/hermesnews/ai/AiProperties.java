package com.hermesnews.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, Duration summaryTimeout) {

	public Duration safeSummaryTimeout() {
		return summaryTimeout == null || summaryTimeout.isNegative() || summaryTimeout.isZero()
				? Duration.ofSeconds(20)
				: summaryTimeout;
	}
}
