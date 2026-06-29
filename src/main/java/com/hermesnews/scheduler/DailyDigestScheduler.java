package com.hermesnews.scheduler;

import com.hermesnews.digest.DailyDigestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyDigestScheduler {

	private final DailyDigestService dailyDigestService;

	public DailyDigestScheduler(DailyDigestService dailyDigestService) {
		this.dailyDigestService = dailyDigestService;
	}

	@Scheduled(cron = "${app.scheduler.daily-digest-cron}", zone = "${app.scheduler.zone}")
	public void sendDailyDigest() {
		dailyDigestService.sendDailyDigest();
	}
}
