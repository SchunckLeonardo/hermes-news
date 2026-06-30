package com.hermesnews.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAgentInterpreter implements AgentInterpreter {

	@Override
	public AgentDecision interpret(String message) {
		return new AgentDecision(
				AgentAction.ANSWER,
				"Estou pronto para ajudar com noticias de tecnologia. O interpretador com IA ainda nao esta ativo.");
	}
}
