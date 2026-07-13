package com.hermesnews.observability;

import com.hermesnews.whatsapp.WhatsAppSendStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class HermesMetrics {

	private final MeterRegistry registry;

	public HermesMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	private HermesMetrics() {
		this.registry = null;
	}

	public static HermesMetrics noop() {
		return new HermesMetrics();
	}

	public void recordDigest(int collected, int selected, WhatsAppSendStatus status) {
		increment("hermes.news.collected", collected);
		increment("hermes.news.selected", selected);
		if (registry != null) {
			registry.counter("hermes.digest.runs", "status", status.name().toLowerCase()).increment();
		}
	}

	public void recordWatchlistScan(int candidates, int alerts) {
		increment("hermes.watchlist.candidates", candidates);
		increment("hermes.watchlist.alerts", alerts);
	}

	public void recordOutboxAttempt(WhatsAppSendStatus status, boolean retry) {
		if (registry != null) {
			registry.counter(
					"hermes.whatsapp.outbox.attempts",
					"status",
					status.name().toLowerCase(),
					"retry",
					Boolean.toString(retry)).increment();
		}
	}

	private void increment(String name, int amount) {
		if (registry != null && amount > 0) {
			registry.counter(name).increment(amount);
		}
	}
}
