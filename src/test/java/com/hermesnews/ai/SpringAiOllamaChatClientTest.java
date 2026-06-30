package com.hermesnews.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiOllamaChatClientTest {

	@Test
	void buildsSpringAiChatClientFromBuilder() {
		var builder = mock(ChatClient.Builder.class);
		var chatClient = mock(ChatClient.class);
		when(builder.build()).thenReturn(chatClient);

		var client = new SpringAiOllamaChatClient(builder);

		assertThat(client).isInstanceOf(AiChatClient.class);
		verify(builder).build();
	}
}
