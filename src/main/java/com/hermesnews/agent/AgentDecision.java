package com.hermesnews.agent;

import com.hermesnews.preferences.PreferenceUpdateRequest;

public record AgentDecision(
		AgentAction action,
		String response,
		PreferenceUpdateRequest preferenceUpdate) {

	public AgentDecision(AgentAction action, String response) {
		this(action, response, null);
	}

	public AgentDecision {
		if (action == null) {
			action = AgentAction.ANSWER;
		}
	}
}
