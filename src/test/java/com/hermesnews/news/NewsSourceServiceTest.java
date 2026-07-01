package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

	@Test
	void listsSourcesWithHealthStatus() {
		var healthy = new NewsSource("healthy.example", NewsSourceType.RSS, "https://healthy.example/feed");
		healthy.recordCollectionSuccess(Instant.parse("2026-07-01T08:00:00Z"));
		var failing = new NewsSource("failing.example", NewsSourceType.RSS, "https://failing.example/feed");
		failing.recordCollectionFailure("Could not parse RSS feed", Instant.parse("2026-07-01T09:00:00Z"));
		when(repository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(healthy, failing));
		var service = new NewsSourceService(repository);

		var sources = service.listSources();

		assertThat(sources).hasSize(2);
		assertThat(sources.get(0).status()).isEqualTo("OK");
		assertThat(sources.get(0).lastSuccessAt()).isEqualTo(Instant.parse("2026-07-01T08:00:00Z"));
		assertThat(sources.get(1).status()).isEqualTo("ERROR");
		assertThat(sources.get(1).lastErrorMessage()).isEqualTo("Could not parse RSS feed");
		assertThat(sources.get(1).consecutiveFailures()).isEqualTo(1);
	}

	@Test
	void recordsCollectionSuccessForExistingSource() {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		source.recordCollectionFailure("previous failure", Instant.parse("2026-07-01T08:00:00Z"));
		when(repository.findByUrl("https://example.com/feed")).thenReturn(Optional.of(source));
		when(repository.save(source)).thenReturn(source);
		var service = new NewsSourceService(repository);

		service.recordCollectionSuccess("https://example.com/feed", Instant.parse("2026-07-01T09:00:00Z"));

		assertThat(source.getLastSuccessAt()).isEqualTo(Instant.parse("2026-07-01T09:00:00Z"));
		assertThat(source.getLastErrorMessage()).isNull();
		assertThat(source.getConsecutiveFailures()).isZero();
		verify(repository).save(source);
	}

	@Test
	void recordsCollectionFailureForExistingSource() {
		var source = new NewsSource("example.com", NewsSourceType.RSS, "https://example.com/feed");
		when(repository.findByUrl("https://example.com/feed")).thenReturn(Optional.of(source));
		when(repository.save(source)).thenReturn(source);
		var service = new NewsSourceService(repository);

		service.recordCollectionFailure("https://example.com/feed", "x".repeat(1200), Instant.parse("2026-07-01T09:00:00Z"));

		assertThat(source.getLastErrorAt()).isEqualTo(Instant.parse("2026-07-01T09:00:00Z"));
		assertThat(source.getLastErrorMessage()).hasSize(1000);
		assertThat(source.getConsecutiveFailures()).isEqualTo(1);
		verify(repository).save(argThat(saved -> saved.getLastErrorMessage().length() == 1000));
	}

	@Test
	void updatesSourceLabelByUrl() {
		var source = new NewsSource("techcrunch.com", NewsSourceType.RSS, "https://techcrunch.com/");
		when(repository.findByUrl("https://techcrunch.com/")).thenReturn(Optional.of(source));
		when(repository.save(source)).thenReturn(source);
		var service = new NewsSourceService(repository);

		var updated = service.updateLabel("https://techcrunch.com/", "TechCrunch");

		assertThat(updated.getName()).isEqualTo("TechCrunch");
		verify(repository).save(source);
	}

	@Test
	void resolvesSourceUrlByEditableLabel() {
		var source = new NewsSource("Akita", NewsSourceType.RSS, "https://akitaonrails.com/en/");
		when(repository.findAllByNameIgnoreCaseOrderByCreatedAtAsc("Akita")).thenReturn(List.of(source));
		var service = new NewsSourceService(repository);

		var url = service.resolveSourceUrl("Akita");

		assertThat(url).isEqualTo("https://akitaonrails.com/en/");
	}

	@Test
	void rejectsBlankSourceLabel() {
		var source = new NewsSource("techcrunch.com", NewsSourceType.RSS, "https://techcrunch.com/");
		when(repository.findByUrl("https://techcrunch.com/")).thenReturn(Optional.of(source));
		var service = new NewsSourceService(repository);

		assertThatThrownBy(() -> service.updateLabel("https://techcrunch.com/", " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("label");
	}
}
