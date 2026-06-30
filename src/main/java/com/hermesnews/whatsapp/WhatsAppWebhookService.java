package com.hermesnews.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermesnews.agent.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppWebhookService {

	public static final String UNAUTHORIZED_SENDER_MESSAGE = "Este assistente e privado.";

	private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);

	private final WhatsAppWebhookEventRepository repository;
	private final ObjectMapper objectMapper;
	private final AgentService agentService;
	private final WhatsAppService whatsAppService;
	private final EvolutionProperties evolutionProperties;

	public WhatsAppWebhookService(
			WhatsAppWebhookEventRepository repository,
			ObjectMapper objectMapper,
			AgentService agentService,
			WhatsAppService whatsAppService,
			EvolutionProperties evolutionProperties) {
		this.repository = repository;
		this.objectMapper = objectMapper;
		this.agentService = agentService;
		this.whatsAppService = whatsAppService;
		this.evolutionProperties = evolutionProperties;
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
		var key = data.path("key");
		var event = payload.path("event").asText("unknown");
		var instance = payload.path("instance").asText("unknown");
		var remoteJid = key.path("remoteJid").asText("");
		if (key.path("fromMe").asBoolean(false)) {
			log.debug("Ignoring WhatsApp webhook event={} instance={} because message is from the connected account",
					event, instance);
			return;
		}
		var text = extractText(data.path("message"));
		var recipient = toRecipient(remoteJid);
		if (!hasText(text)) {
			log.debug("Ignoring WhatsApp webhook event={} instance={} remoteType={} because text is empty",
					event, instance, jidType(remoteJid));
			return;
		}
		if (!hasText(recipient)) {
			log.debug("Ignoring WhatsApp webhook event={} instance={} remoteType={} because recipient is unsupported",
					event, instance, jidType(remoteJid));
			return;
		}
		if (!isAuthorizedSender(recipient)) {
			log.warn("Rejecting WhatsApp inbound text from unauthorized sender instance={} remoteType={}",
					instance, jidType(remoteJid));
			whatsAppService.sendTextTo(recipient, UNAUTHORIZED_SENDER_MESSAGE);
			return;
		}
		log.info("Handling WhatsApp inbound text event={} instance={} remoteType={} textLength={}",
				event, instance, jidType(remoteJid), text.length());
		var response = agentService.handleIncomingText(text);
		if (hasText(response)) {
			var result = whatsAppService.sendTextTo(recipient, response);
			if (result.status() == WhatsAppSendStatus.SENT) {
				log.info("WhatsApp reply sent instance={} remoteType={}", instance, jidType(recipient));
			}
			else {
				log.warn("WhatsApp reply was not sent instance={} remoteType={} status={} detail={}",
						instance, jidType(recipient), result.status(), result.detail());
			}
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
		if (value.endsWith("@g.us") || value.endsWith("@broadcast") || value.endsWith("@newsletter")) {
			return "";
		}
		if (value.endsWith("@lid")) {
			return value;
		}
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

	private boolean isAuthorizedSender(String recipient) {
		var allowedRecipient = evolutionProperties.recipient();
		if (!hasText(allowedRecipient)) {
			return false;
		}
		return normalizeRecipient(recipient).equals(normalizeRecipient(allowedRecipient));
	}

	private static String normalizeRecipient(String value) {
		if (!hasText(value)) {
			return "";
		}
		var normalized = value.trim();
		if (normalized.endsWith("@lid")) {
			return normalized;
		}
		var atIndex = normalized.indexOf('@');
		if (atIndex >= 0) {
			normalized = normalized.substring(0, atIndex);
		}
		var deviceIndex = normalized.indexOf(':');
		if (deviceIndex >= 0) {
			normalized = normalized.substring(0, deviceIndex);
		}
		return normalized.replace("+", "").replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
	}

	private static String jidType(String remoteJid) {
		if (!hasText(remoteJid)) {
			return "empty";
		}
		var value = remoteJid.trim();
		var atIndex = value.lastIndexOf('@');
		if (atIndex < 0 || atIndex == value.length() - 1) {
			return "number";
		}
		return value.substring(atIndex + 1);
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
