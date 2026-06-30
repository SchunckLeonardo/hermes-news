package com.hermesnews.agent;

import com.hermesnews.digest.DailyDigestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

	private static final Logger log = LoggerFactory.getLogger(AgentService.class);

	private final AgentInterpreter interpreter;
	private final DailyDigestService dailyDigestService;

	public AgentService(AgentInterpreter interpreter, DailyDigestService dailyDigestService) {
		this.interpreter = interpreter;
		this.dailyDigestService = dailyDigestService;
	}

	public String handleIncomingText(String message) {
		if (message == null || message.isBlank()) {
			return "Nao consegui entender a mensagem.";
		}
		var decision = interpreter.interpret(message.trim());
		if (decision.action() == AgentAction.SEND_DAILY_DIGEST) {
			return sendDailyDigest(decision.response());
		}
		return hasText(decision.response()) ? decision.response().trim() : "Como posso ajudar com suas noticias de tecnologia?";
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
}
