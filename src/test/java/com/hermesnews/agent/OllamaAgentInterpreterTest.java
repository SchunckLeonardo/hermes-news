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
	void parsesPreferenceUpdateFromAiResponse() {
		var client = new CapturingAiChatClient("""
				{
				  "action": "UPDATE_PREFERENCES",
				  "response": "Preferencias atualizadas.",
				  "preferences": {
				    "addThemes": ["java"],
				    "removeThemes": ["frontend"],
				    "sources": ["infoq"],
				    "newsLimit": 7,
				    "digestTime": "07:30",
				    "language": "pt-BR"
				  }
				}
				""");
		var interpreter = new OllamaAgentInterpreter(client, new ObjectMapper());

		var decision = interpreter.interpret("quero mais noticias de Java e menos frontend");

		assertThat(decision.action()).isEqualTo(AgentAction.UPDATE_PREFERENCES);
		assertThat(decision.preferenceUpdate().addThemes()).containsExactly("java");
		assertThat(decision.preferenceUpdate().removeThemes()).containsExactly("frontend");
		assertThat(decision.preferenceUpdate().sources()).containsExactly("infoq");
		assertThat(decision.preferenceUpdate().newsLimit()).isEqualTo(7);
		assertThat(decision.preferenceUpdate().digestTime().toString()).isEqualTo("07:30");
		assertThat(decision.preferenceUpdate().language()).isEqualTo("pt-BR");
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
