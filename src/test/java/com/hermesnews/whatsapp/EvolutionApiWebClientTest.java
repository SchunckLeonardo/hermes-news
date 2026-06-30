package com.hermesnews.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvolutionApiWebClientTest {

	@Test
	void sendTextPayloadMatchesEvolutionApiSchema() {
		var payload = EvolutionApiWebClient.sendTextPayload("+5511999999999", "ola");

		assertThat(payload)
				.containsEntry("number", "5511999999999")
				.containsEntry("text", "ola");
		assertThat(payload).doesNotContainKey("textMessage");
	}

	@Test
	void sendTextPayloadNormalizesWhatsAppJidToPhoneNumber() {
		var payload = EvolutionApiWebClient.sendTextPayload("5511999999999:12@s.whatsapp.net", "ola");

		assertThat(payload).containsEntry("number", "5511999999999");
	}
}
