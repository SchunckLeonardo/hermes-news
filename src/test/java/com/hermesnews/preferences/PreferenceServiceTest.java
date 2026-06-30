package com.hermesnews.preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

	@Mock
	private PersonalPreferenceRepository repository;

	@Test
	void createsDefaultPreferencesWhenNoneExist() {
		when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
		when(repository.save(any(PersonalPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = new PreferenceService(repository);

		var preferences = service.current();

		assertThat(preferences.themes()).contains("ai", "java", "backend", "cloud");
		assertThat(preferences.newsLimit()).isEqualTo(10);
		assertThat(preferences.digestTime()).isEqualTo(LocalTime.of(8, 0));
		assertThat(preferences.language()).isEqualTo("pt-BR");
		verify(repository).save(any(PersonalPreference.class));
	}

	@Test
	void appliesPreferenceUpdatesByMergingThemesSourcesAndSettings() {
		var existing = PersonalPreference.defaults();
		when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));
		when(repository.save(any(PersonalPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var service = new PreferenceService(repository);

		var updated = service.update(new PreferenceUpdateRequest(
				List.of("java", "spring"),
				List.of("frontend"),
				List.of("infoq", "hacker news"),
				7,
				LocalTime.of(7, 30),
				"pt-BR"));

		assertThat(updated.themes()).contains("java", "spring");
		assertThat(updated.excludedThemes()).contains("frontend");
		assertThat(updated.sources()).containsExactly("infoq", "hacker news");
		assertThat(updated.newsLimit()).isEqualTo(7);
		assertThat(updated.digestTime()).isEqualTo(LocalTime.of(7, 30));
		assertThat(updated.language()).isEqualTo("pt-BR");
		verify(repository).save(existing);
	}
}
