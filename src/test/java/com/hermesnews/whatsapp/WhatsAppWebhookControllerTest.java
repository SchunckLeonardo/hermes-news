package com.hermesnews.whatsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WhatsAppWebhookController.class)
class WhatsAppWebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WhatsAppWebhookService whatsAppWebhookService;

	@Test
	void acceptsWebhookPayload() throws Exception {
		mockMvc.perform(post("/api/whatsapp/webhook")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"event\":\"messages.upsert\",\"instance\":\"personal\"}"))
				.andExpect(status().isAccepted());

		verify(whatsAppWebhookService).record(any(JsonNode.class));
	}
}
