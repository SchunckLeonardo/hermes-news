package com.hermesnews.agent;

import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.preferences.PreferenceService;
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
			- usar as preferencias salvas para priorizar temas e fontes no ranking.
			""".trim();

	private final AgentInterpreter interpreter;
	private final DailyDigestService dailyDigestService;
	private final PreferenceService preferenceService;

	public AgentService(
			AgentInterpreter interpreter,
			DailyDigestService dailyDigestService,
			PreferenceService preferenceService) {
		this.interpreter = interpreter;
		this.dailyDigestService = dailyDigestService;
		this.preferenceService = preferenceService;
	}

	public String handleIncomingText(String message) {
		if (message == null || message.isBlank()) {
			return "Nao consegui entender a mensagem.";
		}
		var normalizedMessage = message.trim();
		if (asksAboutAgent(normalizedMessage)) {
			return CAPABILITIES_MESSAGE;
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

	private static String normalize(String value) {
		return value == null
				? ""
				: value.toLowerCase()
						.replace("é", "e")
						.replace("ê", "e")
						.replace("á", "a")
						.replace("ã", "a")
						.replace("ç", "c")
						.replace("?", "")
						.trim();
	}
}
