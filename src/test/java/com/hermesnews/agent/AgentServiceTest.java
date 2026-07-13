package com.hermesnews.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.DailyDigestResult;
import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.feedback.DigestItemExplanation;
import com.hermesnews.feedback.FeedbackReceipt;
import com.hermesnews.feedback.FeedbackService;
import com.hermesnews.feedback.FeedbackType;
import com.hermesnews.history.HistorySearchResult;
import com.hermesnews.history.NewsHistoryService;
import com.hermesnews.news.Article;
import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsSource;
import com.hermesnews.news.NewsSourceResponse;
import com.hermesnews.news.NewsSourceService;
import com.hermesnews.news.NewsSourceTestResponse;
import com.hermesnews.news.NewsSourceType;
import com.hermesnews.news.RssNewsCollector;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import com.hermesnews.watchlist.WatchlistEntry;
import com.hermesnews.watchlist.WatchlistService;
import java.time.Duration;
import java.time.Instant;
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

	@Mock
	private RssNewsCollector rssNewsCollector;

	@Mock
	private FeedbackService feedbackService;

	@Mock
	private WatchlistService watchlistService;

	@Mock
	private NewsHistoryService newsHistoryService;

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
	void listsSourcesDeterministicallyWithoutCallingAi() {
		when(newsSourceService.listSources()).thenReturn(List.of(new NewsSourceResponse(
				null,
				"example.com",
				NewsSourceType.RSS,
				"https://example.com/feed",
				true,
				null,
				null,
				null,
				null,
				0,
				"UNKNOWN")));
		var service = service();

		var response = service.handleIncomingText("quais fontes estao ativas?");

		assertThat(response)
				.contains("Fontes RSS:")
				.contains("https://example.com/feed")
				.contains("ativa")
				.contains("UNKNOWN");
		verify(newsSourceService).listSources();
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, rssNewsCollector);
	}

	@Test
	void testsRssSourceDeterministicallyWithoutCallingAi() {
		when(rssNewsCollector.testSource("https://example.com/feed"))
				.thenReturn(new NewsSourceTestResponse(
						"https://example.com/feed",
						true,
						2,
						"https://example.com/feed",
						"Source returned 2 article(s)."));
		var service = service();

		var response = service.handleIncomingText("teste a fonte https://example.com/feed");

		assertThat(response)
				.contains("Fonte RSS OK")
				.contains("2 noticias")
				.contains("https://example.com/feed");
		verify(rssNewsCollector).testSource("https://example.com/feed");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void testsRssSourceByLabelWithoutCallingAi() {
		when(newsSourceService.resolveSourceUrl("Akita")).thenReturn("https://akitaonrails.com/en/");
		when(rssNewsCollector.testSource("https://akitaonrails.com/en/"))
				.thenReturn(new NewsSourceTestResponse(
						"https://akitaonrails.com/en/",
						true,
						1,
						"https://akitaonrails.com/en/index.xml",
						"Source returned 1 article(s)."));
		var service = service();

		var response = service.handleIncomingText("teste Akita");

		assertThat(response)
				.contains("Fonte RSS OK")
				.contains("1 noticias")
				.contains("https://akitaonrails.com/en/");
		verify(newsSourceService).resolveSourceUrl("Akita");
		verify(rssNewsCollector).testSource("https://akitaonrails.com/en/");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService);
	}

	@Test
	void disablesRssSourceByLabelWithoutCallingAi() {
		var source = new NewsSource("TechCrunch", NewsSourceType.RSS, "https://techcrunch.com/");
		source.disable();
		when(newsSourceService.disableSource("TechCrunch")).thenReturn(source);
		var service = service();

		var response = service.handleIncomingText("remova TechCrunch");

		assertThat(response).isEqualTo("Fonte RSS desativada: https://techcrunch.com/");
		verify(newsSourceService).disableSource("TechCrunch");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, rssNewsCollector);
	}

	@Test
	void renamesRssSourceByUrlWithoutCallingAi() {
		var source = new NewsSource("techcrunch.com", NewsSourceType.RSS, "https://techcrunch.com/");
		source.rename("TechCrunch");
		when(newsSourceService.updateLabel("https://techcrunch.com/", "TechCrunch")).thenReturn(source);
		var service = service();

		var response = service.handleIncomingText("renomeie fonte https://techcrunch.com/ para TechCrunch");

		assertThat(response).isEqualTo("Fonte RSS renomeada: TechCrunch -> https://techcrunch.com/");
		verify(newsSourceService).updateLabel("https://techcrunch.com/", "TechCrunch");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, rssNewsCollector);
	}

	@Test
	void stripsTrailingColonFromRssSourceUrlBeforeCallingSourceService() {
		var source = new NewsSource("akitaonrails.com", NewsSourceType.RSS, "https://akitaonrails.com/en/");
		when(newsSourceService.addRssSource("https://akitaonrails.com/en/")).thenReturn(source);
		var service = service();

		var response = service.handleIncomingText("adicione fonte https://akitaonrails.com/en/:");

		assertThat(response).isEqualTo("Fonte RSS adicionada: https://akitaonrails.com/en/");
		verify(newsSourceService).addRssSource("https://akitaonrails.com/en/");
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

	@Test
	void recordsPositiveFeedbackForTheLatestDigestWithoutCallingAi() {
		when(feedbackService.recordLatest(2, FeedbackType.POSITIVE))
				.thenReturn(new FeedbackReceipt("OpenAI launches a new model", FeedbackType.POSITIVE));
		var service = service();

		var response = service.handleIncomingText("gostei da noticia 2");

		assertThat(response).contains("Feedback positivo registrado").contains("OpenAI launches a new model");
		verify(feedbackService).recordLatest(2, FeedbackType.POSITIVE);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void recordsNegativeFeedbackForTheLatestDigestWithoutCallingAi() {
		when(feedbackService.recordLatest(4, FeedbackType.NEGATIVE))
				.thenReturn(new FeedbackReceipt("Frontend framework update", FeedbackType.NEGATIVE));
		var service = service();

		var response = service.handleIncomingText("nao gostei da noticia 4");

		assertThat(response).contains("Feedback negativo registrado").contains("Frontend framework update");
		verify(feedbackService).recordLatest(4, FeedbackType.NEGATIVE);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void explainsWhyAnItemWasSelectedWithoutCallingAi() {
		when(feedbackService.explainLatest(3)).thenReturn(new DigestItemExplanation(
				"Java 25 released",
				31,
				"Fonte oficial (+12); Temas preferidos (+5)"));
		var service = service();

		var response = service.handleIncomingText("por que a noticia 3 foi selecionada?");

		assertThat(response)
				.contains("Java 25 released")
				.contains("31")
				.contains("Fonte oficial")
				.contains("Temas preferidos");
		verify(feedbackService).explainLatest(3);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void addsAWatchlistTermWithoutCallingAi() {
		when(watchlistService.add("OpenAI")).thenReturn(new WatchlistEntry("OpenAI", Duration.ofHours(6)));
		var service = service();

		var response = service.handleIncomingText("monitore OpenAI");

		assertThat(response).contains("Monitoramento ativado").contains("openai").contains("6 horas");
		verify(watchlistService).add("OpenAI");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void listsActiveWatchlistTermsWithoutCallingAi() {
		when(watchlistService.activeEntries()).thenReturn(List.of(
				new WatchlistEntry("openai", Duration.ofHours(6)),
				new WatchlistEntry("java", Duration.ofHours(3))));
		var service = service();

		var response = service.handleIncomingText("o que voce esta monitorando?");

		assertThat(response).contains("Monitoramentos ativos:").contains("openai").contains("java");
		verify(watchlistService).activeEntries();
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void removesAWatchlistTermWithoutCallingAi() {
		var entry = new WatchlistEntry("openai", Duration.ofHours(6));
		entry.disable();
		when(watchlistService.remove("OpenAI")).thenReturn(entry);
		var service = service();

		var response = service.handleIncomingText("pare de monitorar OpenAI");

		assertThat(response).isEqualTo("Monitoramento desativado: openai");
		verify(watchlistService).remove("OpenAI");
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	@Test
	void searchesNewsHistoryForTheCurrentWeekWithoutCallingAi() {
		var article = Article.from(new CollectedArticle(
				"OpenAI",
				"history-1",
				"OpenAI launches Sol",
				"https://openai.com/sol",
				"Official launch",
				Instant.parse("2026-07-12T10:00:00Z")), 40);
		when(newsHistoryService.search("OpenAI", Duration.ofDays(7), 5))
				.thenReturn(new HistorySearchResult("OpenAI", Instant.parse("2026-07-06T12:00:00Z"), List.of(article)));
		var service = service();

		var response = service.handleIncomingText("o que saiu sobre OpenAI esta semana?");

		assertThat(response)
				.contains("Historico: OpenAI")
				.contains("OpenAI launches Sol")
				.contains("OpenAI")
				.contains("https://openai.com/sol");
		verify(newsHistoryService).search("OpenAI", Duration.ofDays(7), 5);
		verifyNoInteractions(interpreter, dailyDigestService, preferenceService, newsSourceService);
	}

	private AgentService service() {
		return new AgentService(
				interpreter,
				dailyDigestService,
				preferenceService,
				newsSourceService,
				rssNewsCollector,
				feedbackService,
				watchlistService,
				newsHistoryService);
	}
}
