package com.hermesnews.watchlist;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WatchlistScheduler {

	private final WatchlistService watchlistService;

	public WatchlistScheduler(WatchlistService watchlistService) {
		this.watchlistService = watchlistService;
	}

	@Scheduled(cron = "${app.watchlist.scan-cron:0 */10 * * * *}", zone = "${app.scheduler.zone}")
	public void scan() {
		watchlistService.scanAndAlert();
	}
}
