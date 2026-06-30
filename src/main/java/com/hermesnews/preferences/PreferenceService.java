package com.hermesnews.preferences;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PreferenceService {

	private final PersonalPreferenceRepository repository;

	public PreferenceService(PersonalPreferenceRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public PersonalPreference current() {
		return repository.findFirstByOrderByCreatedAtAsc()
				.orElseGet(() -> repository.save(PersonalPreference.defaults()));
	}

	@Transactional
	public PersonalPreference update(PreferenceUpdateRequest request) {
		var preferences = current();
		preferences.apply(request);
		return repository.save(preferences);
	}
}
