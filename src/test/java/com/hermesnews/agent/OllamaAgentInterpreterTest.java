package com.hermesnews.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermesnews.ai.AiChatClient;
import org.junit.jupiter.api.Test;

class OllamaAgentInterpreterTest {

	@Test
	void asksAiToChooseAnAllowedTool() {
		var client = new CapturingAiChatClient("""
				{"action":"SEND_DAILY_DIGEST","response":"Vou enviar o resumo agora."}
				""");
		var interpreter = new OllamaAgentInterpreter(client, new ObjectMapper());

		var decision = interpreter.interpret("me manda as principais noticias de IA de hoje");

		assertThat(decision.action()).isEqualTo(AgentAction.SEND_DAILY_DIGEST);
		assertThat(decision.response()).isEqualTo("Vou enviar o resumo agora.");
		assertThat(client.systemPrompt)
				.contains("SEND_DAILY_DIGEST")
				.contains("JSON")
				.contains("segredos");
		assertThat(client.userPrompt).contains("me manda as principais noticias de IA de hoje");
	}

	@Test
	void fallsBackToSafeAnswerWhenAiResponseIsInvalid() {
		var interpreter = new OllamaAgentInterpreter((systemPrompt, userPrompt) -> "not-json", new ObjectMapper());

		var decision = interpreter.interpret("qualquer coisa");

		assertThat(decision.action()).isEqualTo(AgentAction.ANSWER);
		assertThat(decision.response()).contains("Nao consegui interpretar");
	}

	private static class CapturingAiChatClient implements AiChatClient {

		private final String response;
		private String systemPrompt;
		private String userPrompt;

		private CapturingAiChatClient(String response) {
			this.response = response;
		}

		@Override
		public String complete(String systemPrompt, String userPrompt) {
			this.systemPrompt = systemPrompt;
			this.userPrompt = userPrompt;
			return response;
		}
	}
}
