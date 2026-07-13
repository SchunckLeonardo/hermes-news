package com.hermesnews.digest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestItemRepository extends JpaRepository<DigestItem, UUID> {

	Optional<DigestItem> findByDigestAndRankOrder(Digest digest, int rankOrder);
}
