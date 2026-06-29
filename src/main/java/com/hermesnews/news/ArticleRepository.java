package com.hermesnews.news;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

	boolean existsByUrl(String url);

	List<Article> findTop10ByOrderByScoreDescPublishedAtDesc();
}
