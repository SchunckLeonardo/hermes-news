package com.hermesnews.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

	private final WhatsAppWebhookService service;

	public WhatsAppWebhookController(WhatsAppWebhookService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, String> receive(@RequestBody JsonNode payload) {
		service.record(payload);
		return Map.of("status", "accepted");
	}
}
