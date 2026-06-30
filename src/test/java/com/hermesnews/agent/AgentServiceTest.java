package com.hermesnews.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.DailyDigestResult;
import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import java.time.LocalTime;
import java.util.List;
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

	@Mock
	private PreferenceService preferenceService;

	@Test
	void runsDailyDigestWhenAiChoosesDigestTool() {
		when(interpreter.interpret("me envia as noticias de IA de hoje"))
				.thenReturn(new AgentDecision(AgentAction.SEND_DAILY_DIGEST, "Vou gerar e enviar o digest agora."));
		when(dailyDigestService.sendDailyDigest())
				.thenReturn(new DailyDigestResult(3, "digest", WhatsAppSendStatus.SENT));
		var service = new AgentService(interpreter, dailyDigestService, preferenceService);

		var response = service.handleIncomingText("me envia as noticias de IA de hoje");

		assertThat(response)
				.contains("Vou gerar e enviar o digest agora.")
				.contains("3 noticias")
				.contains("SENT");
		verify(dailyDigestService).sendDailyDigest();
	}

	@Test
	void returnsExactCapabilitiesWithoutCallingAiWhenUserAsksAboutAgent() {
		var service = new AgentService(interpreter, dailyDigestService, preferenceService);

		var response = service.handleIncomingText("quem e voce?");

		assertThat(response).isEqualTo(AgentService.CAPABILITIES_MESSAGE);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService);
	}

	@Test
	void updatesPreferencesWhenAiChoosesPreferenceTool() {
		var request = new PreferenceUpdateRequest(
				List.of("java"),
				List.of("frontend"),
				List.of(),
				null,
				null,
				null);
		when(interpreter.interpret("quero mais noticias de Java e menos frontend"))
				.thenReturn(new AgentDecision(AgentAction.UPDATE_PREFERENCES, "Preferencias atualizadas.", request));
		when(preferenceService.update(request)).thenReturn(PersonalPreference.defaults());
		var service = new AgentService(interpreter, dailyDigestService, preferenceService);

		var response = service.handleIncomingText("quero mais noticias de Java e menos frontend");

		assertThat(response).contains("Preferencias atualizadas.");
		verify(preferenceService).update(request);
		verifyNoInteractions(dailyDigestService);
	}

	@Test
	void returnsAiResponseWhenNoToolIsNeeded() {
		when(interpreter.interpret("responda algo simples"))
				.thenReturn(new AgentDecision(AgentAction.ANSWER, "Resposta direta."));
		var service = new AgentService(interpreter, dailyDigestService, preferenceService);

		var response = service.handleIncomingText("responda algo simples");

		assertThat(response).isEqualTo("Resposta direta.");
		verifyNoInteractions(dailyDigestService, preferenceService);
	}
}
