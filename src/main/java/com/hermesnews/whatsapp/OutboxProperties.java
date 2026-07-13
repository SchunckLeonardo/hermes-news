package com.hermesnews.whatsapp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("app.outbox")
public record OutboxProperties(int maxAttempts, Duration baseDelay) {

	@ConstructorBinding
	public OutboxProperties {
		maxAttempts = Math.max(1, maxAttempts);
		baseDelay = baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()
				? Duration.ofMinutes(1)
				: baseDelay;
	}
}
