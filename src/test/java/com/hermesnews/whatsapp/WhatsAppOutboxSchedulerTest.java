package com.hermesnews.whatsapp;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WhatsAppOutboxSchedulerTest {

	@Test
	void delegatesRetryProcessingToTheOutboxService() {
		var service = Mockito.mock(OutboxWhatsAppService.class);
		var scheduler = new WhatsAppOutboxScheduler(service);

		scheduler.retryDueMessages();

		verify(service).retryDue();
	}
}
