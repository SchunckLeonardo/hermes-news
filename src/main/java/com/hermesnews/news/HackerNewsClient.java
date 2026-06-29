package com.hermesnews.news;

import java.util.List;
import java.util.Optional;

public interface HackerNewsClient {

	List<Long> topStories();

	Optional<HackerNewsItem> item(long id);
}
