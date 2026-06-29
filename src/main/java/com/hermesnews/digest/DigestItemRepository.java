package com.hermesnews.digest;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestItemRepository extends JpaRepository<DigestItem, UUID> {
}
