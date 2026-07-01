package com.hermesnews.scheduler;

import com.hermesnews.digest.DailyDigestService;
import com.hermesnews.preferences.PreferenceService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyDigestScheduler {

	private final DailyDigestService dailyDigestService;
	private final PreferenceService preferenceService;
	private final SchedulerProperties properties;
	private final Clock clock;
	private LocalDate lastRunDate;

	public DailyDigestScheduler(
			DailyDigestService dailyDigestService,
			PreferenceService preferenceService,
			SchedulerProperties properties,
			Clock clock) {
		this.dailyDigestService = dailyDigestService;
		this.preferenceService = preferenceService;
		this.properties = properties;
		this.clock = clock;
	}

	@Scheduled(cron = "${app.scheduler.digest-check-cron:0 * * * * *}", zone = "${app.scheduler.zone}")
	public void sendDailyDigest() {
		sendDailyDigestIfPreferredTime();
	}

	void sendDailyDigestIfPreferredTime() {
		var now = clock.instant().atZone(properties.zoneId());
		var today = now.toLocalDate();
		var preferredTime = preferenceService.current().digestTime().truncatedTo(ChronoUnit.MINUTES);
		var currentTime = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
		if (!currentTime.equals(preferredTime) || today.equals(lastRunDate)) {
			return;
		}
		dailyDigestService.sendDailyDigest();
		lastRunDate = today;
	}
}
