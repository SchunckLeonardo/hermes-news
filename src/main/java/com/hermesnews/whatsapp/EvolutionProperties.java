package com.hermesnews.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.evolution")
public record EvolutionProperties(
		String baseUrl,
		String apiKey,
		String instance,
		String recipient,
		String allowedSender) {

	public boolean isComplete() {
		return hasBaseConfiguration() && hasText(recipient);
	}

	public boolean hasBaseConfiguration() {
		return hasText(baseUrl) && hasText(apiKey) && hasText(instance);
	}

	public EvolutionProperties withRecipient(String recipient) {
		return new EvolutionProperties(baseUrl, apiKey, instance, recipient, allowedSender);
	}

	public String allowedSenderOrRecipient() {
		if (hasText(allowedSender)) {
			return allowedSender;
		}
		return recipient;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
