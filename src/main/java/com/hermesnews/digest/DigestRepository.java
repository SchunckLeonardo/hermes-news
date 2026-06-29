package com.hermesnews.digest;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestRepository extends JpaRepository<Digest, UUID> {
}
