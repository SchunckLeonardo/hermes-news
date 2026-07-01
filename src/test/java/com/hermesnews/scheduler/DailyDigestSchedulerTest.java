package com.hermesnews.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DailyDigestSchedulerTest {

	@Test
	void sendsDigestOnceWhenCurrentTimeMatchesPreferredDigestTime() {
		var digestService = Mockito.mock(DailyDigestService.class);
		var preferenceService = Mockito.mock(PreferenceService.class);
		var preferences = PersonalPreference.defaults();
		preferences.apply(new PreferenceUpdateRequest(List.of(), List.of(), List.of(), null, LocalTime.of(8, 30), null));
		when(preferenceService.current()).thenReturn(preferences);
		var scheduler = new DailyDigestScheduler(
				digestService,
				preferenceService,
				new SchedulerProperties("America/Sao_Paulo"),
				Clock.fixed(Instant.parse("2026-06-30T11:30:00Z"), ZoneOffset.UTC));

		scheduler.sendDailyDigestIfPreferredTime();
		scheduler.sendDailyDigestIfPreferredTime();

		verify(digestService).sendDailyDigest();
	}

	@Test
	void doesNotSendDigestWhenCurrentTimeDoesNotMatchPreferredDigestTime() {
		var digestService = Mockito.mock(DailyDigestService.class);
		var preferenceService = Mockito.mock(PreferenceService.class);
		when(preferenceService.current()).thenReturn(PersonalPreference.defaults());
		var scheduler = new DailyDigestScheduler(
				digestService,
				preferenceService,
				new SchedulerProperties("America/Sao_Paulo"),
				Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC));

		scheduler.sendDailyDigestIfPreferredTime();

		verify(digestService, never()).sendDailyDigest();
	}
}
