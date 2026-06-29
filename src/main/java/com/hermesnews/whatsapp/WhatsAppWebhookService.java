package com.hermesnews.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppWebhookService {

	private final WhatsAppWebhookEventRepository repository;
	private final ObjectMapper objectMapper;

	public WhatsAppWebhookService(WhatsAppWebhookEventRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	public WhatsAppWebhookEvent record(JsonNode payload) {
		var event = payload.path("event").asText("unknown");
		var instance = payload.path("instance").asText(null);
		return repository.save(new WhatsAppWebhookEvent(event, instance, toJson(payload)));
	}

	private String toJson(JsonNode payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException exception) {
			return "{}";
		}
	}
}
