package com.hermesnews.news;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, UUID> {

	Optional<NewsSource> findByUrl(String url);

	List<NewsSource> findAllByEnabledTrueAndType(NewsSourceType type);

	List<NewsSource> findAllByOrderByCreatedAtAsc();

	List<NewsSource> findAllByNameIgnoreCaseOrderByCreatedAtAsc(String name);
}
