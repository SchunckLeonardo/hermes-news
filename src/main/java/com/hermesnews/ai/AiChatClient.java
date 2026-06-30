package com.hermesnews.ai;

public interface AiChatClient {

	String complete(String systemPrompt, String userPrompt);
}
