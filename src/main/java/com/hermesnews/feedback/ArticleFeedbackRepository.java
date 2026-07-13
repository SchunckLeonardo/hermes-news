package com.hermesnews.feedback;

import com.hermesnews.news.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleFeedbackRepository extends JpaRepository<ArticleFeedback, UUID> {

	Optional<ArticleFeedback> findByArticle(Article article);

	List<ArticleFeedback> findTop100ByOrderByUpdatedAtDesc();
}
