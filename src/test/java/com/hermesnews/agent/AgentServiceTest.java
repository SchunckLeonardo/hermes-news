package com.hermesnews.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.DailyDigestResult;
import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

	@Mock
	private AgentInterpreter interpreter;

	@Mock
	private DailyDigestService dailyDigestService;

	@Test
	void runsDailyDigestWhenAiChoosesDigestTool() {
		when(interpreter.interpret("me envia as noticias de IA de hoje"))
				.thenReturn(new AgentDecision(AgentAction.SEND_DAILY_DIGEST, "Vou gerar e enviar o digest agora."));
		when(dailyDigestService.sendDailyDigest())
				.thenReturn(new DailyDigestResult(3, "digest", WhatsAppSendStatus.SENT));
		var service = new AgentService(interpreter, dailyDigestService);

		var response = service.handleIncomingText("me envia as noticias de IA de hoje");

		assertThat(response)
				.contains("Vou gerar e enviar o digest agora.")
				.contains("3 noticias")
				.contains("SENT");
		verify(dailyDigestService).sendDailyDigest();
	}

	@Test
	void returnsAiResponseWhenNoToolIsNeeded() {
		when(interpreter.interpret("quem e voce?"))
				.thenReturn(new AgentDecision(AgentAction.ANSWER, "Sou o Hermes News."));
		var service = new AgentService(interpreter, dailyDigestService);

		var response = service.handleIncomingText("quem e voce?");

		assertThat(response).isEqualTo("Sou o Hermes News.");
		verifyNoInteractions(dailyDigestService);
	}
}
