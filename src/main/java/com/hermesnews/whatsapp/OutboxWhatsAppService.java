package com.hermesnews.whatsapp;

import com.hermesnews.observability.HermesMetrics;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxWhatsAppService implements WhatsAppService {

	private final WhatsAppOutboxRepository repository;
	private final WhatsAppGateway gateway;
	private final EvolutionProperties evolutionProperties;
	private final OutboxProperties properties;
	private final Clock clock;
	private final HermesMetrics metrics;

	public OutboxWhatsAppService(
			WhatsAppOutboxRepository repository,
			WhatsAppGateway gateway,
			EvolutionProperties evolutionProperties,
			OutboxProperties properties,
			Clock clock,
			HermesMetrics metrics) {
		this.repository = repository;
		this.gateway = gateway;
		this.evolutionProperties = evolutionProperties;
		this.properties = properties;
		this.clock = clock;
		this.metrics = metrics;
	}

	@Override
	@Transactional
	public WhatsAppSendResult sendText(String message) {
		return sendTextTo(evolutionProperties.recipient(), message);
	}

	@Override
	@Transactional
	public WhatsAppSendResult sendTextTo(String recipient, String message) {
		var outbox = repository.save(new WhatsAppOutboxMessage(
				recipient,
				message,
				properties.maxAttempts(),
				clock.instant()));
		return attempt(outbox, false);
	}

	@Transactional
	public int retryDue() {
		var now = clock.instant();
		var due = repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
				WhatsAppOutboxStatus.FAILED,
				now);
		var retried = 0;
		for (WhatsAppOutboxMessage message : due) {
			if (!message.isRetryable()) {
				continue;
			}
			attempt(message, true);
			retried++;
		}
		return retried;
	}

	private WhatsAppSendResult attempt(WhatsAppOutboxMessage outbox, boolean retry) {
		WhatsAppSendResult result;
		try {
			result = gateway.sendTextTo(outbox.getRecipient(), outbox.getMessage());
		}
		catch (RuntimeException exception) {
			result = WhatsAppSendResult.failed("WhatsApp gateway failed: " + exception.getClass().getSimpleName());
		}
		outbox.apply(result, clock.instant(), properties.baseDelay());
		repository.save(outbox);
		metrics.recordOutboxAttempt(result.status(), retry);
		return result;
	}
}
