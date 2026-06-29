package com.hermesnews.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.evolution")
public record EvolutionProperties(
		String baseUrl,
		String apiKey,
		String instance,
		String recipient) {

	public boolean isComplete() {
		return hasText(baseUrl) && hasText(apiKey) && hasText(instance) && hasText(recipient);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
