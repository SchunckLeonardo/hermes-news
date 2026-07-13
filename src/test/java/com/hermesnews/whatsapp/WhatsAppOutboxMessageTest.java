package com.hermesnews.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WhatsAppOutboxMessageTest {

	@Test
	void stopsSchedulingRetriesAfterTheMaximumAttemptCount() {
		var now = Instant.parse("2026-07-13T12:00:00Z");
		var message = new WhatsAppOutboxMessage("5511999999999", "message", 2, now);

		message.apply(WhatsAppSendResult.failed("first"), now, Duration.ofMinutes(1));
		assertThat(message.getNextAttemptAt()).isEqualTo(now.plus(Duration.ofMinutes(1)));

		message.apply(WhatsAppSendResult.failed("second"), now.plus(Duration.ofMinutes(1)), Duration.ofMinutes(1));

		assertThat(message.getAttempts()).isEqualTo(2);
		assertThat(message.getStatus()).isEqualTo(WhatsAppOutboxStatus.FAILED);
		assertThat(message.getNextAttemptAt()).isNull();
		assertThat(message.isRetryable()).isFalse();
	}

	@Test
	void neverRetriesSkippedMessages() {
		var now = Instant.parse("2026-07-13T12:00:00Z");
		var message = new WhatsAppOutboxMessage("", "message", 3, now);

		message.apply(WhatsAppSendResult.skipped("configuration incomplete"), now, Duration.ofMinutes(1));

		assertThat(message.getStatus()).isEqualTo(WhatsAppOutboxStatus.SKIPPED);
		assertThat(message.getNextAttemptAt()).isNull();
		assertThat(message.isRetryable()).isFalse();
	}
}
