package com.hermesnews.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermesnews.agent.AgentService;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppWebhookService {

	private final WhatsAppWebhookEventRepository repository;
	private final ObjectMapper objectMapper;
	private final AgentService agentService;
	private final WhatsAppService whatsAppService;

	public WhatsAppWebhookService(
			WhatsAppWebhookEventRepository repository,
			ObjectMapper objectMapper,
			AgentService agentService,
			WhatsAppService whatsAppService) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.agentService = agentService;
		this.whatsAppService = whatsAppService;
	}

	public WhatsAppWebhookEvent record(JsonNode payload) {
		var event = payload.path("event").asText("unknown");
		var instance = payload.path("instance").asText(null);
		var saved = repository.save(new WhatsAppWebhookEvent(event, instance, toJson(payload)));
		handleInboundText(payload);
		return saved;
	}

	private void handleInboundText(JsonNode payload) {
		var data = payload.path("data");
		if (data.path("key").path("fromMe").asBoolean(false)) {
			return;
		}
		var text = extractText(data.path("message"));
		var recipient = toRecipient(data.path("key").path("remoteJid").asText(""));
		if (!hasText(text) || !hasText(recipient)) {
			return;
		}
		var response = agentService.handleIncomingText(text);
		if (hasText(response)) {
			whatsAppService.sendTextTo(recipient, response);
		}
	}

	private static String extractText(JsonNode message) {
		var conversation = message.path("conversation").asText("");
		if (hasText(conversation)) {
			return conversation.trim();
		}
		var extended = message.path("extendedTextMessage").path("text").asText("");
		if (hasText(extended)) {
			return extended.trim();
		}
		var caption = message.path("imageMessage").path("caption").asText("");
		if (hasText(caption)) {
			return caption.trim();
		}
		return "";
	}

	private static String toRecipient(String remoteJid) {
		if (!hasText(remoteJid)) {
			return "";
		}
		var value = remoteJid.trim();
		var atIndex = value.indexOf('@');
		if (atIndex >= 0) {
			value = value.substring(0, atIndex);
		}
		var deviceIndex = value.indexOf(':');
		if (deviceIndex >= 0) {
			value = value.substring(0, deviceIndex);
		}
		return value;
	}

	private String toJson(JsonNode payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
