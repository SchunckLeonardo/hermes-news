package com.hermesnews.news;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, UUID> {
}
