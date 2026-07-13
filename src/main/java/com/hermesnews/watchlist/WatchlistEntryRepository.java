package com.hermesnews.watchlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, UUID> {

	Optional<WatchlistEntry> findByTermIgnoreCase(String term);

	List<WatchlistEntry> findAllByEnabledTrueOrderByTermAsc();
}
