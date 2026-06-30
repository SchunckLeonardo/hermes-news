package com.hermesnews.preferences;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalPreferenceRepository extends JpaRepository<PersonalPreference, UUID> {

	Optional<PersonalPreference> findFirstByOrderByCreatedAtAsc();
}
