package com.hermesnews.scheduler;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(String zone) {

	public ZoneId zoneId() {
		return ZoneId.of(zone == null || zone.isBlank() ? "America/Sao_Paulo" : zone);
	}
}
