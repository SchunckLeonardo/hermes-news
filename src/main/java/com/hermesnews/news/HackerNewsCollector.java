package com.hermesnews.news;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class HackerNewsCollector implements NewsCollector {

	private final HackerNewsClient client;
	private final HackerNewsProperties properties;

	public HackerNewsCollector(HackerNewsClient client, HackerNewsProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	@Override
	public List<CollectedArticle> collect() {
		int maxItems = properties.maxItems() <= 0 ? 10 : properties.maxItems();
		return client.topStories().stream()
				.limit(maxItems)
				.map(client::item)
				.flatMap(OptionalSupport::stream)
				.filter(this::isUsable)
				.map(this::toCollectedArticle)
				.toList();
	}

	private boolean isUsable(HackerNewsItem item) {
		return !Boolean.TRUE.equals(item.dead())
				&& !Boolean.TRUE.equals(item.deleted())
				&& item.title() != null
				&& !item.title().isBlank()
				&& item.url() != null
				&& !item.url().isBlank();
	}

	private CollectedArticle toCollectedArticle(HackerNewsItem item) {
		return new CollectedArticle(
				"hacker-news",
				Objects.toString(item.id(), null),
				item.title(),
				item.url(),
				"",
				item.time() == null ? null : Instant.ofEpochSecond(item.time()));
	}

	private static final class OptionalSupport {
		private OptionalSupport() {
		}

		private static <T> java.util.stream.Stream<T> stream(java.util.Optional<T> optional) {
			return optional.stream();
		}
	}
}
