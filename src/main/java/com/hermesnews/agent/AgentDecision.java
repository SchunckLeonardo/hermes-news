package com.hermesnews.agent;

public record AgentDecision(
		AgentAction action,
		String response) {

	public AgentDecision {
		if (action == null) {
			action = AgentAction.ANSWER;
		}
	}
}
