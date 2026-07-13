package com.hermesnews.watchlist;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrgentAlertRepository extends JpaRepository<UrgentAlert, UUID> {

	boolean existsByArticleUrl(String articleUrl);
}
