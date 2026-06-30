package com.hermesnews.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EvolutionApiWebClientTest {

	@Test
	void sendTextPayloadMatchesEvolutionApiSchema() {
		var payload = EvolutionApiWebClient.sendTextPayload("+5511999999999", "ola");

		assertThat(payload)
				.containsEntry("number", "5511999999999")
				.containsKey("textMessage");
		assertThat(payload).doesNotContainKey("text");
		assertThat(payload.get("textMessage"))
				.isInstanceOf(Map.class)
				.extracting(value -> ((Map<?, ?>) value).get("text"))
				.isEqualTo("ola");
	}

	@Test
	void sendTextPayloadNormalizesWhatsAppJidToPhoneNumber() {
		var payload = EvolutionApiWebClient.sendTextPayload("5511999999999:12@s.whatsapp.net", "ola");

		assertThat(payload).containsEntry("number", "5511999999999");
	}
}
