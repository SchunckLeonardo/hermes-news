package com.hermesnews.digest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hermesnews.whatsapp.WhatsAppSendStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DigestController.class)
class DigestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DailyDigestService dailyDigestService;

	@Test
	void postSendDailyTriggersDigest() throws Exception {
		when(dailyDigestService.sendDailyDigest())
				.thenReturn(new DailyDigestResult(2, "digest message", WhatsAppSendStatus.SKIPPED));

		mockMvc.perform(post("/api/digests/send-daily"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.articleCount").value(2))
				.andExpect(jsonPath("$.whatsAppStatus").value("SKIPPED"));
	}
}
