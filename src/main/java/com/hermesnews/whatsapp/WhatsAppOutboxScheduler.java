package com.hermesnews.whatsapp;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppOutboxScheduler {

	private final OutboxWhatsAppService service;

	public WhatsAppOutboxScheduler(OutboxWhatsAppService service) {
		this.service = service;
	}

	@Scheduled(cron = "${app.outbox.retry-cron:0 * * * * *}", zone = "${app.scheduler.zone}")
	public void retryDueMessages() {
		service.retryDue();
	}
}
