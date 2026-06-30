package com.hermesnews.whatsapp;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Component
public class EvolutionApiWebClient implements EvolutionApiClient {

	private static final Logger log = LoggerFactory.getLogger(EvolutionApiWebClient.class);

	private final WebClient.Builder builder;

	public EvolutionApiWebClient(WebClient.Builder builder) {
		this.builder = builder;
	}

	@Override
	public WhatsAppSendResult sendText(EvolutionProperties properties, String message) {
		try {
			builder.baseUrl(properties.baseUrl())
					.build()
					.post()
					.uri("/message/sendText/{instance}", properties.instance())
					.header("apikey", properties.apiKey())
					.header(HttpHeaders.CONTENT_TYPE, "application/json")
					.bodyValue(sendTextPayload(properties.recipient(), message))
					.retrieve()
					.toBodilessEntity()
					.block(Duration.ofSeconds(15));
			return WhatsAppSendResult.sent();
		}
		catch (WebClientException exception) {
			log.warn("Evolution API send failed: {}", exception.getMessage());
			return WhatsAppSendResult.failed("Evolution API send failed");
		}
	}

	static Map<String, Object> sendTextPayload(String recipient, String message) {
		return Map.of(
				"number", toEvolutionNumber(recipient),
				"textMessage", Map.of("text", message));
	}

	static String toEvolutionNumber(String recipient) {
		if (recipient == null || recipient.isBlank()) {
			return "";
		}
		var value = recipient.trim();
		var atIndex = value.indexOf('@');
		if (atIndex >= 0) {
			value = value.substring(0, atIndex);
		}
		var deviceIndex = value.indexOf(':');
		if (deviceIndex >= 0) {
			value = value.substring(0, deviceIndex);
		}
		return value.replaceAll("[^0-9]", "");
	}
}
