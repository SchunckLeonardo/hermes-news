package com.hermesnews.news;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

	boolean existsByUrl(String url);

	List<Article> findTop10ByOrderByScoreDescPublishedAtDesc();

	@Query("""
			select article from Article article
			where article.publishedAt >= :since
			  and (
			    lower(article.title) like concat('%', :query, '%')
			    or lower(coalesce(article.summary, '')) like concat('%', :query, '%')
			    or lower(article.sourceName) like concat('%', :query, '%')
			  )
			order by article.publishedAt desc, article.score desc
			""")
	List<Article> searchHistory(@Param("query") String query, @Param("since") Instant since, Pageable pageable);
}
