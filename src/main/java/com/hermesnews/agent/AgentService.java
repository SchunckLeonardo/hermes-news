package com.hermesnews.agent;

import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.feedback.FeedbackService;
import com.hermesnews.feedback.FeedbackType;
import com.hermesnews.history.NewsHistoryService;
import com.hermesnews.news.NewsSourceResponse;
import com.hermesnews.news.NewsSourceService;
import com.hermesnews.news.NewsSourceTestResponse;
import com.hermesnews.news.RssNewsCollector;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import com.hermesnews.watchlist.WatchlistService;
import java.time.LocalTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

	private static final Logger log = LoggerFactory.getLogger(AgentService.class);

	public static final String CAPABILITIES_MESSAGE = """
			Sou o Hermes News. Hoje eu posso:
			- gerar e enviar o digest diario de noticias de tecnologia;
			- responder perguntas simples sobre o proprio agente;
			- atualizar preferencias pessoais de temas, fontes, quantidade de noticias, horario preferido e idioma;
			- usar as preferencias salvas para priorizar temas e fontes no ranking;
			- registrar feedback positivo ou negativo sobre itens do ultimo digest;
			- explicar os sinais que fizeram uma noticia entrar no ranking;
			- monitorar termos e enviar alertas urgentes com cooldown;
			- buscar noticias ja entregues por tema em periodos recentes;
			- listar, testar, ativar, desativar e cadastrar fontes RSS publicas.
			""".trim();

	private final AgentInterpreter interpreter;
	private final DailyDigestService dailyDigestService;
	private final PreferenceService preferenceService;
	private final NewsSourceService newsSourceService;
	private final RssNewsCollector rssNewsCollector;
	private final FeedbackService feedbackService;
	private final WatchlistService watchlistService;
	private final NewsHistoryService newsHistoryService;

	public AgentService(
			AgentInterpreter interpreter,
			DailyDigestService dailyDigestService,
			PreferenceService preferenceService,
			NewsSourceService newsSourceService,
			RssNewsCollector rssNewsCollector,
			FeedbackService feedbackService,
			WatchlistService watchlistService,
			NewsHistoryService newsHistoryService) {
		this.interpreter = interpreter;
		this.dailyDigestService = dailyDigestService;
		this.preferenceService = preferenceService;
		this.newsSourceService = newsSourceService;
		this.rssNewsCollector = rssNewsCollector;
		this.feedbackService = feedbackService;
		this.watchlistService = watchlistService;
		this.newsHistoryService = newsHistoryService;
	}

	public String handleIncomingText(String message) {
		if (message == null || message.isBlank()) {
			return "Nao consegui entender a mensagem.";
		}
		var normalizedMessage = message.trim();
		if (asksAboutAgent(normalizedMessage)) {
			return CAPABILITIES_MESSAGE;
		}
		if (asksForPreferences(normalizedMessage)) {
			return currentPreferencesMessage();
		}
		if (asksForSources(normalizedMessage)) {
			return sourcesMessage();
		}
		var feedbackResponse = handleFeedbackOrExplanation(normalizedMessage);
		if (hasText(feedbackResponse)) {
			return feedbackResponse;
		}
		var historyResponse = handleHistorySearch(normalizedMessage);
		if (hasText(historyResponse)) {
			return historyResponse;
		}
		var watchlistResponse = handleWatchlistCommand(normalizedMessage);
		if (hasText(watchlistResponse)) {
			return watchlistResponse;
		}
		var sourceResponse = handleSourceCommand(normalizedMessage);
		if (hasText(sourceResponse)) {
			return sourceResponse;
		}
		if (asksForDigest(normalizedMessage)) {
			return sendDailyDigest("");
		}
		var preferenceUpdate = parsePreferenceUpdate(normalizedMessage);
		if (preferenceUpdate != null) {
			preferenceService.update(preferenceUpdate);
			return "Preferencias atualizadas.";
		}
		var decision = interpreter.interpret(normalizedMessage);
		if (decision.action() == AgentAction.SEND_DAILY_DIGEST) {
			return sendDailyDigest(decision.response());
		}
		if (decision.action() == AgentAction.UPDATE_PREFERENCES) {
			return updatePreferences(decision);
		}
		if (decision.action() == AgentAction.SHOW_CAPABILITIES) {
			return CAPABILITIES_MESSAGE;
		}
		return hasText(decision.response()) ? decision.response().trim() : "Como posso ajudar com suas noticias de tecnologia?";
	}

	private String handleHistorySearch(String message) {
		var normalized = normalize(message);
		if (!containsAny(normalized,
				"o que saiu sobre ",
				"busque noticias sobre ",
				"buscar noticias sobre ",
				"procure noticias sobre ",
				"historico de ")) {
			return "";
		}
		var marker = List.of(
				"o que saiu sobre ",
				"busque noticias sobre ",
				"buscar noticias sobre ",
				"procure noticias sobre ",
				"historico de ").stream()
				.filter(normalized::contains)
				.findFirst()
				.orElse("");
		var start = normalized.indexOf(marker) + marker.length();
		var query = message.substring(start)
				.replaceAll("[?!.;:]+$", "")
				.replaceAll("(?i)\\s+(esta semana|hoje|no ultimo mes|nos ultimos 30 dias)$", "")
				.trim();
		var period = normalized.contains("hoje")
				? Duration.ofDays(1)
				: containsAny(normalized, "ultimo mes", "ultimos 30 dias") ? Duration.ofDays(30) : Duration.ofDays(7);
		try {
			var result = newsHistoryService.search(query, period, 5);
			if (result.articles().isEmpty()) {
				return "Nao encontrei noticias recentes sobre " + result.query() + ".";
			}
			var lines = new ArrayList<String>();
			lines.add("*Historico: " + result.query() + "*");
			var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));
			for (int index = 0; index < result.articles().size(); index++) {
				var article = result.articles().get(index);
				var date = article.getPublishedAt() == null ? "data desconhecida" : formatter.format(article.getPublishedAt());
				lines.add("%d. *%s*\nData: %s\nFonte: %s\nLink: %s".formatted(
						index + 1,
						article.getTitle(),
						date,
						article.getSourceName(),
						article.getUrl()));
			}
			return String.join("\n\n", lines);
		}
		catch (IllegalArgumentException exception) {
			return "Informe um tema valido para pesquisar no historico.";
		}
	}

	private String handleWatchlistCommand(String message) {
		var normalized = normalize(message);
		try {
			if (containsAny(normalized,
					"o que voce esta monitorando",
					"listar monitoramentos",
					"monitoramentos ativos",
					"minha watchlist")) {
				var entries = watchlistService.activeEntries();
				if (entries.isEmpty()) {
					return "Nenhum monitoramento urgente esta ativo.";
				}
				var lines = new ArrayList<String>();
				lines.add("Monitoramentos ativos:");
				for (var entry : entries) {
					lines.add("- %s (cooldown: %d horas)".formatted(
							entry.getTerm(),
							Math.max(1, entry.getCooldownMinutes() / 60)));
				}
				return String.join("\n", lines);
			}
			for (String prefix : List.of("pare de monitorar ", "remova da watchlist ", "remover da watchlist ")) {
				if (normalized.startsWith(prefix)) {
					var term = message.substring(prefix.length()).trim();
					var entry = watchlistService.remove(term);
					return "Monitoramento desativado: " + entry.getTerm();
				}
			}
			for (String prefix : List.of("monitore ", "monitorar ", "acompanhe ", "adicione a watchlist ")) {
				if (normalized.startsWith(prefix)) {
					var term = message.substring(prefix.length()).trim();
					var entry = watchlistService.add(term);
					return "Monitoramento ativado: %s (cooldown: %d horas).".formatted(
							entry.getTerm(),
							Math.max(1, entry.getCooldownMinutes() / 60));
				}
			}
			return "";
		}
		catch (IllegalArgumentException exception) {
			return "Nao consegui atualizar esse monitoramento.";
		}
	}

	private String handleFeedbackOrExplanation(String message) {
		var normalized = normalize(message);
		var position = firstInteger(normalized);
		if (position == null) {
			return "";
		}
		try {
			if (containsAny(normalized, "por que", "porque")
					&& containsAny(normalized, "noticia", "selecionada", "ranking")) {
				var explanation = feedbackService.explainLatest(position);
				return "*%s*\nPontuacao: %d\nMotivos: %s".formatted(
						explanation.articleTitle(),
						explanation.score(),
						explanation.explanation());
			}
			if (containsAny(normalized, "nao gostei", "irrelevante", "nao curti")) {
				var receipt = feedbackService.recordLatest(position, FeedbackType.NEGATIVE);
				return "Feedback negativo registrado para: " + receipt.articleTitle();
			}
			if (containsAny(normalized, "gostei", "curti", "relevante")) {
				var receipt = feedbackService.recordLatest(position, FeedbackType.POSITIVE);
				return "Feedback positivo registrado para: " + receipt.articleTitle();
			}
			return "";
		}
		catch (IllegalArgumentException exception) {
			return "Nao encontrei essa noticia no ultimo digest.";
		}
	}

	private String currentPreferencesMessage() {
		var preferences = preferenceService.current();
		var activeSources = newsSourceService.enabledRssUrls();
		return """
				Suas preferencias atuais:
				- Temas: %s
				- Menos prioridade: %s
				- Fontes priorizadas: %s
				- Fontes RSS ativas: %s
				- Quantidade: %d noticias
				- Horario: %s
				- Idioma: %s
				""".formatted(
				formatList(preferences.themes()),
				formatList(preferences.excludedThemes()),
				formatList(preferences.sources()),
				formatList(activeSources),
				preferences.newsLimit(),
				preferences.digestTime(),
				preferences.language()).trim();
	}

	private String handleSourceCommand(String message) {
		var normalized = normalize(message);
		if (!containsAny(normalized,
				"fonte",
				"rss",
				"feed",
				"teste",
				"testar",
				"valide",
				"validar",
				"desative",
				"desativar",
				"remova",
				"remover",
				"ative",
				"ativar",
				"renomeie",
				"renomear")) {
			return "";
		}
		var labelUpdate = parseSourceLabelUpdate(message);
		var reference = labelUpdate == null ? sourceReference(message) : labelUpdate.reference();
		if (!hasText(reference)) {
			return "";
		}
		try {
			if (labelUpdate != null) {
				var source = newsSourceService.updateLabel(labelUpdate.reference(), labelUpdate.label());
				return "Fonte RSS renomeada: " + source.getName() + " -> " + source.getUrl();
			}
			if (containsAny(normalized, "teste", "testar", "valide", "validar")) {
				var testUrl = looksLikeHttpUrl(reference) ? reference : newsSourceService.resolveSourceUrl(reference);
				return testSourceMessage(rssNewsCollector.testSource(testUrl));
			}
			if (containsAny(normalized, "desative", "desativar", "remova", "remover")) {
				var source = newsSourceService.disableSource(reference);
				return "Fonte RSS desativada: " + source.getUrl();
			}
			if (containsAny(normalized, "ative", "ativar", "reative", "reativar")) {
				var source = newsSourceService.enableSource(reference);
				return "Fonte RSS ativada: " + source.getUrl();
			}
			if (looksLikeHttpUrl(reference)) {
				var source = newsSourceService.addRssSource(reference);
				return "Fonte RSS adicionada: " + source.getUrl();
			}
			return "";
		}
		catch (IllegalArgumentException exception) {
			return "Nao consegui salvar essa fonte. Envie uma URL publica http ou https de RSS.";
		}
	}

	private String sourcesMessage() {
		var sources = newsSourceService.listSources();
		if (sources.isEmpty()) {
			return "Nenhuma fonte RSS cadastrada.";
		}
		var lines = new ArrayList<String>();
		lines.add("Fontes RSS:");
		for (NewsSourceResponse source : sources) {
			lines.add("- %s [%s, %s]".formatted(source.url(), source.enabled() ? "ativa" : "inativa", source.status()));
		}
		return String.join("\n", lines);
	}

	private static String testSourceMessage(NewsSourceTestResponse result) {
		if (result.success()) {
			return "Fonte RSS OK: %s (%d noticias).".formatted(result.url(), result.articleCount());
		}
		return "Fonte RSS com falha: %s - %s".formatted(result.url(), result.message());
	}

	private static SourceLabelUpdate parseSourceLabelUpdate(String message) {
		var normalized = normalize(message);
		if (!containsAny(normalized, "renomeie", "renomear")) {
			return null;
		}
		var separatorIndex = normalized.indexOf(" para ");
		if (separatorIndex < 0) {
			return null;
		}
		var referencePart = message.substring(0, separatorIndex).trim();
		var label = message.substring(separatorIndex + " para ".length()).trim();
		var reference = sourceReference(referencePart);
		return hasText(reference) && hasText(label) ? new SourceLabelUpdate(reference, label) : null;
	}

	private static String sourceReference(String message) {
		var url = firstUrl(message);
		if (hasText(url)) {
			return url;
		}
		var normalized = normalize(message);
		for (String prefix : List.of(
				"teste a fonte ",
				"teste fonte ",
				"testar a fonte ",
				"testar fonte ",
				"teste ",
				"valide a fonte ",
				"valide fonte ",
				"validar fonte ",
				"desative a fonte ",
				"desative fonte ",
				"desativar fonte ",
				"remova a fonte ",
				"remova fonte ",
				"remover fonte ",
				"remova ",
				"ative a fonte ",
				"ative fonte ",
				"ativar fonte ",
				"reative fonte ",
				"renomeie a fonte ",
				"renomeie fonte ",
				"renomear fonte ")) {
			if (normalized.startsWith(prefix) && message.length() >= prefix.length()) {
				return message.substring(prefix.length()).trim();
			}
		}
		return "";
	}

	private static PreferenceUpdateRequest parsePreferenceUpdate(String message) {
		var normalized = normalize(message);
		var addThemes = new ArrayList<String>();
		var removeThemes = new ArrayList<String>();
		var preferredSources = new ArrayList<String>();

		var moreIndex = normalized.indexOf("mais noticias de ");
		if (moreIndex >= 0) {
			addThemes.addAll(splitTerms(sectionAfter(normalized, moreIndex + "mais noticias de ".length())));
		}
		var lessIndex = normalized.indexOf("menos ");
		if (lessIndex >= 0) {
			removeThemes.addAll(splitTerms(sectionAfter(normalized, lessIndex + "menos ".length())));
		}
		var prioritizeIndex = normalized.indexOf("priorize ");
		if (prioritizeIndex >= 0) {
			preferredSources.addAll(splitTerms(sectionAfter(normalized, prioritizeIndex + "priorize ".length())));
		}
		var newsLimit = firstIntegerBeforeWord(normalized, "noticias");
		var digestTime = firstTime(normalized);
		var language = normalized.contains("ingles") || normalized.contains("english")
				? "en"
				: normalized.contains("portugues") || normalized.contains("pt-br") ? "pt-BR" : null;

		if (addThemes.isEmpty() && removeThemes.isEmpty() && preferredSources.isEmpty()
				&& newsLimit == null && digestTime == null && language == null) {
			return null;
		}
		return new PreferenceUpdateRequest(addThemes, removeThemes, preferredSources, newsLimit, digestTime, language);
	}

	private String updatePreferences(AgentDecision decision) {
		try {
			preferenceService.update(decision.preferenceUpdate());
			return hasText(decision.response()) ? decision.response().trim() : "Preferencias atualizadas.";
		}
		catch (RuntimeException exception) {
			log.warn("Agent preference update failed: {}", exception.getMessage());
			return "Tentei atualizar suas preferencias, mas ocorreu uma falha local. Verifique os logs da aplicacao.";
		}
	}

	private String sendDailyDigest(String responsePrefix) {
		try {
			var result = dailyDigestService.sendDailyDigest();
			var prefix = hasText(responsePrefix) ? responsePrefix.trim() + "\n\n" : "";
			return prefix + "Digest processado: " + result.articleCount() + " noticias, envio WhatsApp " + result.whatsAppStatus() + ".";
		}
		catch (RuntimeException exception) {
			log.warn("Agent daily digest tool failed: {}", exception.getMessage());
			return "Tentei gerar o digest, mas ocorreu uma falha local. Verifique os logs da aplicacao.";
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static boolean asksAboutAgent(String message) {
		var normalized = normalize(message);
		return normalized.contains("quem e voce")
				|| normalized.contains("quem voce e")
				|| normalized.contains("o que voce faz")
				|| normalized.contains("sobre o agente")
				|| normalized.contains("suas capacidades")
				|| normalized.contains("voce consegue fazer");
	}

	private static boolean asksForPreferences(String message) {
		var normalized = normalize(message);
		return normalized.contains("minhas preferencias")
				|| normalized.contains("preferencias atuais")
				|| normalized.contains("quais sao minhas preferencias")
				|| normalized.contains("mostrar preferencias");
	}

	private static boolean asksForSources(String message) {
		var normalized = normalize(message);
		return containsAny(normalized, "fontes ativas", "fontes rss", "listar fontes", "quais fontes", "mostrar fontes");
	}

	private static boolean asksForDigest(String message) {
		var normalized = normalize(message);
		return normalized.contains("me envia o digest")
				|| normalized.contains("me envie o digest")
				|| normalized.contains("me envia as noticias")
				|| normalized.contains("me envie as noticias")
				|| normalized.contains("me manda as noticias")
				|| normalized.contains("me mande as noticias")
				|| normalized.contains("mande o digest")
				|| normalized.contains("enviar digest")
				|| normalized.contains("noticias de hoje")
				|| normalized.contains("resumo de hoje");
	}

	private static String firstUrl(String message) {
		for (String token : message.split("\\s+")) {
			var cleaned = token.strip().replaceAll("[),.;:]+$", "");
			if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
				return cleaned;
			}
		}
		return "";
	}

	private static boolean looksLikeHttpUrl(String value) {
		var normalized = value == null ? "" : value.strip().toLowerCase();
		return normalized.startsWith("http://") || normalized.startsWith("https://");
	}

	private record SourceLabelUpdate(String reference, String label) {
	}

	private static boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private static String formatList(List<String> values) {
		return values == null || values.isEmpty() ? "nenhuma" : String.join(", ", values);
	}

	private static String sectionAfter(String value, int start) {
		var section = value.substring(start);
		for (String delimiter : List.of(" e menos ", ",", ".", ";")) {
			var index = section.indexOf(delimiter);
			if (index >= 0) {
				section = section.substring(0, index);
			}
		}
		return section.trim();
	}

	private static List<String> splitTerms(String value) {
		if (!hasText(value)) {
			return List.of();
		}
		var terms = new ArrayList<String>();
		for (String item : value.split("\\s+e\\s+|,|/")) {
			var normalized = normalize(item)
					.replace("noticias", "")
					.replace("por dia", "")
					.trim();
			if (hasText(normalized) && !terms.contains(normalized)) {
				terms.add(normalized);
			}
		}
		return List.copyOf(terms);
	}

	private static Integer firstIntegerBeforeWord(String value, String word) {
		var tokens = value.split("\\s+");
		for (int index = 0; index < tokens.length - 1; index++) {
			if (!tokens[index + 1].startsWith(word)) {
				continue;
			}
			try {
				return Integer.parseInt(tokens[index]);
			}
			catch (NumberFormatException ignored) {
				continue;
			}
		}
		return null;
	}

	private static Integer firstInteger(String value) {
		for (String token : value.split("[^0-9]+")) {
			if (token.isBlank()) {
				continue;
			}
			try {
				return Integer.parseInt(token);
			}
			catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private static LocalTime firstTime(String value) {
		for (String token : value.split("\\s+")) {
			if (!token.matches("\\d{1,2}:\\d{2}")) {
				continue;
			}
			try {
				return LocalTime.parse(token.length() == 4 ? "0" + token : token);
			}
			catch (DateTimeParseException ignored) {
				return null;
			}
		}
		return null;
	}

	private static String normalize(String value) {
		return value == null
				? ""
				: value.toLowerCase()
						.replace("é", "e")
						.replace("ê", "e")
						.replace("á", "a")
						.replace("ã", "a")
						.replace("í", "i")
						.replace("ó", "o")
						.replace("ç", "c")
						.replace("?", "")
						.trim();
	}
}
