package com.hermesnews.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvolutionWhatsAppServiceTest {

	@Mock
	private EvolutionApiClient client;

	@Test
	void skipsSendWhenCredentialsAreMissing() {
		var service = new EvolutionWhatsAppService(new EvolutionProperties("", "", "", "", ""), client);

		var result = service.sendText("hello");

		assertThat(result.status()).isEqualTo(WhatsAppSendStatus.SKIPPED);
		verifyNoInteractions(client);
	}

	@Test
	void sendsWhenConfigurationIsComplete() {
		var properties = new EvolutionProperties("https://evolution.example", "key", "personal", "+5511999999999", "");
		when(client.sendText(properties, "digest")).thenReturn(WhatsAppSendResult.sent());
		var service = new EvolutionWhatsAppService(properties, client);

		var result = service.sendText("digest");

		assertThat(result.status()).isEqualTo(WhatsAppSendStatus.SENT);
		verify(client).sendText(properties, "digest");
	}

	@Test
	void skipsSendWhenRecipientIsLidBecauseSendTextRequiresPhoneNumber() {
		var properties = new EvolutionProperties("https://evolution.example", "key", "personal", "24155080654903@lid", "");
		var service = new EvolutionWhatsAppService(properties, client);

		var result = service.sendText("digest");

		assertThat(result.status()).isEqualTo(WhatsAppSendStatus.SKIPPED);
		assertThat(result.detail()).contains("phone number");
		verifyNoInteractions(client);
	}
}
