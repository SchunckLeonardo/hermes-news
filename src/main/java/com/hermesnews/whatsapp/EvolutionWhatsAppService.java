package com.hermesnews.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EvolutionWhatsAppService implements WhatsAppGateway {

	private static final Logger log = LoggerFactory.getLogger(EvolutionWhatsAppService.class);

	private final EvolutionProperties properties;
	private final EvolutionApiClient client;

	public EvolutionWhatsAppService(EvolutionProperties properties, EvolutionApiClient client) {
		this.properties = properties;
		this.client = client;
	}

	@Override
	public WhatsAppSendResult sendText(String message) {
		return sendTextTo(properties.recipient(), message);
	}

	@Override
	public WhatsAppSendResult sendTextTo(String recipient, String message) {
		if (message == null || message.isBlank()) {
			return WhatsAppSendResult.skipped("message is empty");
		}
		if (recipient == null || recipient.isBlank()) {
			return WhatsAppSendResult.skipped("recipient is empty");
		}
		if (isLidRecipient(recipient)) {
			log.warn("Skipping WhatsApp send because Evolution sendText requires a phone number recipient");
			return WhatsAppSendResult.skipped("recipient must be a phone number for Evolution sendText");
		}
		if (!properties.hasBaseConfiguration()) {
			log.info("Skipping WhatsApp send because Evolution API configuration is incomplete");
			return WhatsAppSendResult.skipped("Evolution API configuration is incomplete");
		}
		return client.sendText(properties.withRecipient(recipient), message);
	}

	private static boolean isLidRecipient(String recipient) {
		return recipient.trim().endsWith("@lid");
	}
}
