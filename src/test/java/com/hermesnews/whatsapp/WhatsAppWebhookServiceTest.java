package com.hermesnews.whatsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermesnews.agent.AgentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceTest {

	@Mock
	private WhatsAppWebhookEventRepository repository;

	@Mock
	private AgentService agentService;

	@Mock
	private WhatsAppService whatsAppService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsInboundTextMessageToAgentAndRepliesToSender() throws Exception {
		when(repository.findByInstanceNameAndMessageId("hermes-local", "3A123")).thenReturn(Optional.empty());
		when(repository.save(any(WhatsAppWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(agentService.handleIncomingText("me manda noticias de IA")).thenReturn("Vou enviar o digest.");
		when(whatsAppService.sendTextTo("5511999999999", WhatsAppWebhookService.PROCESSING_ACK_MESSAGE))
				.thenReturn(WhatsAppSendResult.sent());
		when(whatsAppService.sendTextTo("5511999999999", "Vou enviar o digest.")).thenReturn(WhatsAppSendResult.sent());
		var service = serviceWithRecipient("5511999999999");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "instance": "hermes-local",
				  "data": {
				    "key": {
				      "id": "3A123",
				      "remoteJid": "5511999999999@s.whatsapp.net",
				      "fromMe": false
				    },
				    "message": {
				      "conversation": "me manda noticias de IA"
				    }
				  }
				}
				"""));

		verify(agentService).handleIncomingText("me manda noticias de IA");
		var ordered = inOrder(whatsAppService, agentService);
		ordered.verify(whatsAppService).sendTextTo("5511999999999", WhatsAppWebhookService.PROCESSING_ACK_MESSAGE);
		ordered.verify(agentService).handleIncomingText("me manda noticias de IA");
		ordered.verify(whatsAppService).sendTextTo("5511999999999", "Vou enviar o digest.");
	}

	@Test
	void ignoresDuplicatedWebhookMessageIds() throws Exception {
		var existing = new WhatsAppWebhookEvent(
				"messages.upsert",
				"hermes-local",
				"{}",
				"3A_DUPLICATED",
				"5511999999999@s.whatsapp.net",
				false);
		when(repository.findByInstanceNameAndMessageId("hermes-local", "3A_DUPLICATED"))
				.thenReturn(Optional.of(existing));
		var service = serviceWithRecipient("5511999999999");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "instance": "hermes-local",
				  "data": {
				    "key": {
				      "id": "3A_DUPLICATED",
				      "remoteJid": "5511999999999@s.whatsapp.net",
				      "fromMe": false
				    },
				    "message": {
				      "conversation": "me responde uma vez"
				    }
				  }
				}
				"""));

		verify(repository, never()).save(any(WhatsAppWebhookEvent.class));
		verifyNoInteractions(agentService, whatsAppService);
	}

	@Test
	void repliesToConfiguredRecipientWhenAllowedSenderIsLid() throws Exception {
		when(repository.save(any(WhatsAppWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(agentService.handleIncomingText("quais sao as noticias de hoje?")).thenReturn("Aqui esta o resumo.");
		when(whatsAppService.sendTextTo("5511999999999", WhatsAppWebhookService.PROCESSING_ACK_MESSAGE))
				.thenReturn(WhatsAppSendResult.sent());
		when(whatsAppService.sendTextTo("5511999999999", "Aqui esta o resumo."))
				.thenReturn(WhatsAppSendResult.sent());
		var service = serviceWithRecipientAndAllowedSender("5511999999999", "24155080654903@lid");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "instance": "hermes-local",
				  "data": {
				    "key": {
				      "remoteJid": "24155080654903@lid",
				      "fromMe": false
				    },
				    "message": {
				      "conversation": "quais sao as noticias de hoje?"
				    }
				  }
				}
				"""));

		verify(agentService).handleIncomingText("quais sao as noticias de hoje?");
		verify(whatsAppService).sendTextTo("5511999999999", WhatsAppWebhookService.PROCESSING_ACK_MESSAGE);
		verify(whatsAppService).sendTextTo("5511999999999", "Aqui esta o resumo.");
	}

	@Test
	void ignoresMessagesSentByTheConnectedAccount() throws Exception {
		when(repository.save(any(WhatsAppWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = serviceWithRecipient("5511999999999");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "data": {
				    "key": {
				      "remoteJid": "5511999999999@s.whatsapp.net",
				      "fromMe": true
				    },
				    "message": {
				      "conversation": "loop"
				    }
				  }
				}
				"""));

		verifyNoInteractions(agentService, whatsAppService);
	}

	@Test
	void ignoresGroupMessages() throws Exception {
		when(repository.save(any(WhatsAppWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = serviceWithRecipient("5511999999999");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "data": {
				    "key": {
				      "remoteJid": "120363427566272026@g.us",
				      "fromMe": false
				    },
				    "message": {
				      "conversation": "me responda no grupo"
				    }
				  }
				}
				"""));

		verifyNoInteractions(agentService, whatsAppService);
	}

	@Test
	void rejectsInboundTextFromUnauthorizedSenderWithFixedMessage() throws Exception {
		when(repository.save(any(WhatsAppWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(whatsAppService.sendTextTo("5511888888888", WhatsAppWebhookService.UNAUTHORIZED_SENDER_MESSAGE))
				.thenReturn(WhatsAppSendResult.sent());
		var service = serviceWithRecipient("5511999999999");

		service.record(objectMapper.readTree("""
				{
				  "event": "messages.upsert",
				  "instance": "hermes-local",
				  "data": {
				    "key": {
				      "remoteJid": "5511888888888@s.whatsapp.net",
				      "fromMe": false
				    },
				    "message": {
				      "conversation": "oi"
				    }
				  }
				}
				"""));

		verifyNoInteractions(agentService);
		verify(whatsAppService).sendTextTo("5511888888888", WhatsAppWebhookService.UNAUTHORIZED_SENDER_MESSAGE);
	}

	private WhatsAppWebhookService serviceWithRecipient(String recipient) {
		return serviceWithRecipientAndAllowedSender(recipient, recipient);
	}

	private WhatsAppWebhookService serviceWithRecipientAndAllowedSender(String recipient, String allowedSender) {
		var properties = new EvolutionProperties("http://evolution", "key", "hermes-local", recipient, allowedSender);
		return new WhatsAppWebhookService(repository, objectMapper, agentService, whatsAppService, properties);
	}
}
