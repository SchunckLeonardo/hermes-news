package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsSourceServiceTest {

	@Mock
	private NewsSourceRepository repository;

	@Test
	void addsValidatedRssSourceWhenUrlIsPublicHttpUrl() {
		when(repository.findByUrl("https://news.ycombinator.com/rss")).thenReturn(Optional.empty());
		when(repository.save(any(NewsSource.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = new NewsSourceService(repository);

		var source = service.addRssSource("https://news.ycombinator.com/rss");

		assertThat(source.getUrl()).isEqualTo("https://news.ycombinator.com/rss");
		assertThat(source.getType()).isEqualTo(NewsSourceType.RSS);
		assertThat(source.isEnabled()).isTrue();
		verify(repository).save(any(NewsSource.class));
	}

	@Test
	void removesTrailingChatPunctuationBeforePersistingSourceUrl() {
		when(repository.findByUrl("https://akitaonrails.com/en/")).thenReturn(Optional.empty());
		when(repository.save(any(NewsSource.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = new NewsSourceService(repository);

		var source = service.addRssSource("https://akitaonrails.com/en/:");

		assertThat(source.getUrl()).isEqualTo("https://akitaonrails.com/en/");
		verify(repository).findByUrl("https://akitaonrails.com/en/");
		verify(repository).save(any(NewsSource.class));
	}

	@Test
	void rejectsLocalOrPrivateUrls() {
		var service = new NewsSourceService(repository);

		assertThatThrownBy(() -> service.addRssSource("http://localhost:8080/feed"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public http");
		assertThatThrownBy(() -> service.addRssSource("http://192.168.0.10/feed"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("public http");
	}

	@Test
	void disablesAndEnablesExistingSource() {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		when(repository.findByUrl("https://example.com/feed")).thenReturn(Optional.of(source));
		when(repository.save(source)).thenReturn(source);
		var service = new NewsSourceService(repository);

		var disabled = service.disableSource("https://example.com/feed");
		assertThat(disabled.isEnabled()).isFalse();
		var enabled = service.enableSource("https://example.com/feed");

		assertThat(enabled.isEnabled()).isTrue();
		verify(repository, times(2)).save(source);
	}

	@Test
	void returnsEnabledRssUrls() {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		when(repository.findAllByEnabledTrueAndType(NewsSourceType.RSS)).thenReturn(List.of(source));
		var service = new NewsSourceService(repository);

		assertThat(service.enabledRssUrls()).containsExactly("https://example.com/feed");
	}
}
