package com.hermesnews.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hermesnews.observability.HermesMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxWhatsAppServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");
	private static final EvolutionProperties EVOLUTION = new EvolutionProperties(
			"http://evolution",
			"key",
			"hermes-local",
			"5511999999999",
			"5511999999999");

	@Mock
	private WhatsAppOutboxRepository repository;

	@Mock
	private WhatsAppGateway gateway;

	@Test
	void persistsBeforeSendingAndMarksSuccessfulDelivery() {
		when(repository.save(any(WhatsAppOutboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(gateway.sendTextTo("5511999999999", "digest")).thenReturn(WhatsAppSendResult.sent());
		var registry = new SimpleMeterRegistry();
		var service = service(Clock.fixed(NOW, ZoneOffset.UTC), registry);

		var result = service.sendText("digest");

		assertThat(result.status()).isEqualTo(WhatsAppSendStatus.SENT);
		var captor = ArgumentCaptor.forClass(WhatsAppOutboxMessage.class);
		verify(repository, atLeast(2)).save(captor.capture());
		var message = captor.getValue();
		assertThat(message.getStatus()).isEqualTo(WhatsAppOutboxStatus.SENT);
		assertThat(message.getAttempts()).isEqualTo(1);
		assertThat(message.getSentAt()).isEqualTo(NOW);
		assertThat(registry.get("hermes.whatsapp.outbox.attempts")
				.tag("status", "sent")
				.tag("retry", "false")
				.counter().count()).isEqualTo(1);
	}

	@Test
	void retriesDueFailedMessagesAndMarksThemAsSent() {
		when(repository.save(any(WhatsAppOutboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(gateway.sendTextTo("5511999999999", "alert"))
				.thenReturn(WhatsAppSendResult.failed("temporary failure"))
				.thenReturn(WhatsAppSendResult.sent());
		var registry = new SimpleMeterRegistry();
		var initialService = service(Clock.fixed(NOW, ZoneOffset.UTC), registry);

		initialService.sendText("alert");
		var captor = ArgumentCaptor.forClass(WhatsAppOutboxMessage.class);
		verify(repository, atLeast(2)).save(captor.capture());
		var failed = captor.getValue();
		assertThat(failed.getStatus()).isEqualTo(WhatsAppOutboxStatus.FAILED);
		assertThat(failed.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));

		var retryAt = NOW.plus(Duration.ofMinutes(1));
		when(repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
				WhatsAppOutboxStatus.FAILED,
				retryAt)).thenReturn(List.of(failed));
		var retryService = service(Clock.fixed(retryAt, ZoneOffset.UTC), registry);

		var retried = retryService.retryDue();

		assertThat(retried).isEqualTo(1);
		assertThat(failed.getStatus()).isEqualTo(WhatsAppOutboxStatus.SENT);
		assertThat(failed.getAttempts()).isEqualTo(2);
		assertThat(registry.get("hermes.whatsapp.outbox.attempts")
				.tag("status", "sent")
				.tag("retry", "true")
				.counter().count()).isEqualTo(1);
	}

	private OutboxWhatsAppService service(Clock clock, SimpleMeterRegistry registry) {
		return new OutboxWhatsAppService(
				repository,
				gateway,
				EVOLUTION,
				new OutboxProperties(3, Duration.ofMinutes(1)),
				clock,
				new HermesMetrics(registry));
	}
}
