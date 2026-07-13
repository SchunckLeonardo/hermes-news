package com.hermesnews.watchlist;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WatchlistSchedulerTest {

	@Test
	void delegatesScheduledScanToWatchlistService() {
		var service = Mockito.mock(WatchlistService.class);
		var scheduler = new WatchlistScheduler(service);

		scheduler.scan();

		verify(service).scanAndAlert();
	}
}
