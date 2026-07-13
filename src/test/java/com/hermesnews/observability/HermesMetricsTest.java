package com.hermesnews.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.whatsapp.WhatsAppSendStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class HermesMetricsTest {

	@Test
	void recordsDigestAndWatchlistMeasurements() {
		var registry = new SimpleMeterRegistry();
		var metrics = new HermesMetrics(registry);

		metrics.recordDigest(12, 5, WhatsAppSendStatus.SENT);
		metrics.recordWatchlistScan(3, 1);

		assertThat(registry.get("hermes.news.collected").counter().count()).isEqualTo(12);
		assertThat(registry.get("hermes.news.selected").counter().count()).isEqualTo(5);
		assertThat(registry.get("hermes.digest.runs").tag("status", "sent").counter().count()).isEqualTo(1);
		assertThat(registry.get("hermes.watchlist.alerts").counter().count()).isEqualTo(1);
	}
}
