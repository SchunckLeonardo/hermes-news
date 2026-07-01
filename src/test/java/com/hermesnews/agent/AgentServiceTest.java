package com.hermesnews.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.DailyDigestResult;
import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.news.NewsSource;
import com.hermesnews.news.NewsSourceService;
import com.hermesnews.news.NewsSourceType;
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

	@Mock
	private NewsSourceService newsSourceService;

	@Test
	void runsDailyDigestDeterministicallyWithoutCallingAi() {
		when(dailyDigestService.sendDailyDigest())
				.thenReturn(new DailyDigestResult(3, "digest", WhatsAppSendStatus.SENT));
		var service = service();

		var response = service.handleIncomingText("me envia as noticias de IA de hoje");

		assertThat(response)
				.contains("3 noticias")
				.contains("SENT");
		verify(dailyDigestService).sendDailyDigest();
		verifyNoInteractions(interpreter, preferenceService, newsSourceService);
	}

	@Test
	void returnsExactCapabilitiesWithoutCallingAiWhenUserAsksAboutAgent() {
		var service = service();

		var response = service.handleIncomingText("quem e voce?");

		assertThat(response).isEqualTo(AgentService.CAPABILITIES_MESSAGE);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void showsCurrentPreferencesWithoutCallingAi() {
		var preferences = PersonalPreference.defaults();
		preferences.apply(new PreferenceUpdateRequest(List.of("spring"), List.of("frontend"), List.of("infoq"), 7,
				LocalTime.of(7, 30), "pt-BR"));
		when(preferenceService.current()).thenReturn(preferences);
		when(newsSourceService.enabledRssUrls()).thenReturn(List.of("https://news.ycombinator.com/rss"));
		var service = service();

		var response = service.handleIncomingText("quais sao minhas preferencias?");

		assertThat(response)
				.contains("Temas: ai, java, backend, cloud, spring")
				.contains("Menos prioridade: frontend")
				.contains("Fontes priorizadas: infoq")
				.contains("Fontes RSS ativas: https://news.ycombinator.com/rss")
				.contains("Quantidade: 7")
				.contains("Horario: 07:30")
				.contains("Idioma: pt-BR");
		verifyNoInteractions(interpreter, dailyDigestService);
	}

	@Test
	void updatesPreferencesDeterministicallyWithoutCallingAi() {
		var request = new PreferenceUpdateRequest(
				List.of("java"),
				List.of("frontend"),
				List.of(),
				7,
				null,
				null);
		when(preferenceService.update(request)).thenReturn(PersonalPreference.defaults());
		var service = service();

		var response = service.handleIncomingText("quero mais noticias de Java e menos frontend, 7 noticias por dia");

		assertThat(response).contains("Preferencias atualizadas.");
		verify(preferenceService).update(request);
		verifyNoInteractions(interpreter, dailyDigestService, newsSourceService);
	}

	@Test
	void addsRssSourceDeterministicallyWithoutCallingAi() {
		var source = new NewsSource("news.ycombinator.com", NewsSourceType.RSS, "https://news.ycombinator.com/rss");
		when(newsSourceService.addRssSource("https://news.ycombinator.com/rss")).thenReturn(source);
		var service = service();

		var response = service.handleIncomingText("adicione fonte https://news.ycombinator.com/rss");

		assertThat(response).isEqualTo("Fonte RSS adicionada: https://news.ycombinator.com/rss");
		verify(newsSourceService).addRssSource("https://news.ycombinator.com/rss");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService);
	}

	@Test
	void returnsAiResponseWhenNoToolIsNeeded() {
		when(interpreter.interpret("responda algo simples"))
				.thenReturn(new AgentDecision(AgentAction.ANSWER, "Resposta direta."));
		var service = service();

		var response = service.handleIncomingText("responda algo simples");

		assertThat(response).isEqualTo("Resposta direta.");
		verifyNoInteractions(dailyDigestService, preferenceService, newsSourceService);
	}

	private AgentService service() {
		return new AgentService(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}
}
