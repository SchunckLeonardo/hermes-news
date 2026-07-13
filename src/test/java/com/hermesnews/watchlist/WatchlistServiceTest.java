package com.hermesnews.watchlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsCollector;
import com.hermesnews.observability.HermesMetrics;
import com.hermesnews.ranking.RankingProperties;
import com.hermesnews.ranking.RankingService;
import com.hermesnews.ranking.SemanticEventClusterer;
import com.hermesnews.whatsapp.WhatsAppSendResult;
import com.hermesnews.whatsapp.WhatsAppService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

	private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");

	@Mock
	private WatchlistEntryRepository entryRepository;

	@Mock
	private UrgentAlertRepository alertRepository;

	@Mock
	private NewsCollector collector;

	@Mock
	private WhatsAppService whatsAppService;

	@Mock
	private HermesMetrics metrics;

	private WatchlistService service;

	@BeforeEach
	void setUp() {
		var ranking = new RankingService(
				new RankingProperties(
						List.of("ai"),
						List.of("openai.com"),
						List.of("openai", "sol"),
						List.of("launches")),
				null,
				Clock.fixed(NOW, ZoneOffset.UTC));
		service = new WatchlistService(
				List.of(collector),
				ranking,
				new SemanticEventClusterer(),
				entryRepository,
				alertRepository,
				whatsAppService,
				Clock.fixed(NOW, ZoneOffset.UTC),
				new WatchlistProperties(Duration.ofHours(6), Duration.ofHours(24), 20, 2),
				metrics);
	}

	@Test
	void persistsANormalizedWatchTermUsingTheDefaultCooldown() {
		when(entryRepository.findByTermIgnoreCase("OpenAI")).thenReturn(Optional.empty());
		when(entryRepository.save(any(WatchlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var entry = service.add(" OpenAI ");

		assertThat(entry.getTerm()).isEqualTo("openai");
		assertThat(entry.getCooldownMinutes()).isEqualTo(360);
		assertThat(entry.isEnabled()).isTrue();
	}

	@Test
	void sendsTheHighestRankedFreshMatchAndStartsItsCooldown() {
		var entry = new WatchlistEntry("openai", Duration.ofHours(6));
		var article = new CollectedArticle(
				"OpenAI",
				"urgent-1",
				"OpenAI launches Sol model today",
				"https://openai.com/news/sol",
				"Official AI launch for developers",
				NOW.minus(Duration.ofMinutes(20)));
		when(entryRepository.findAllByEnabledTrueOrderByTermAsc()).thenReturn(List.of(entry));
		when(collector.collect()).thenReturn(List.of(article));
		when(alertRepository.existsByArticleUrl(article.url())).thenReturn(false);
		when(whatsAppService.sendText(contains("OpenAI launches Sol"))).thenReturn(WhatsAppSendResult.sent());
		when(alertRepository.save(any(UrgentAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(entryRepository.save(any(WatchlistEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = service.scanAndAlert();

		assertThat(result.alertCount()).isEqualTo(1);
		assertThat(entry.getLastAlertedAt()).isEqualTo(NOW);
		verify(alertRepository).save(any(UrgentAlert.class));
		verify(whatsAppService).sendText(contains("*Hermes Alerta*"));
		verify(metrics).recordWatchlistScan(1, 1);
	}

	@Test
	void doesNotCollectOrSendWhileEveryMatchingTermIsCoolingDown() {
		var entry = new WatchlistEntry("openai", Duration.ofHours(6));
		entry.markAlerted(NOW.minus(Duration.ofHours(1)));
		when(entryRepository.findAllByEnabledTrueOrderByTermAsc()).thenReturn(List.of(entry));

		var result = service.scanAndAlert();

		assertThat(result.alertCount()).isZero();
		verify(collector, never()).collect();
		verifyNoInteractions(whatsAppService, alertRepository);
		verify(metrics).recordWatchlistScan(0, 0);
	}
}
