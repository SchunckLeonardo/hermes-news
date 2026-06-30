package com.hermesnews.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama")
public class SpringAiOllamaChatClient implements AiChatClient {

	private final ChatClient chatClient;

	public SpringAiOllamaChatClient(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		return chatClient.prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.call()
				.content();
	}
}
