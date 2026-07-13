package com.hermesnews.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.hermesnews.ai.AiSummaryService;
import com.hermesnews.news.Article;
import com.hermesnews.news.ArticleRepository;
import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsCollector;
import com.hermesnews.preferences.PersonalPreference;
import com.hermesnews.preferences.PreferenceService;
import com.hermesnews.preferences.PreferenceUpdateRequest;
import com.hermesnews.ranking.RankingProperties;
import com.hermesnews.ranking.RankingService;
import com.hermesnews.ranking.RankedArticle;
import com.hermesnews.whatsapp.WhatsAppSendResult;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import com.hermesnews.whatsapp.WhatsAppService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyDigestServiceTest {

	@Mock
	private NewsCollector collector;

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private DigestRepository digestRepository;

	@Mock
	private DigestItemRepository digestItemRepository;

	@Mock
	private AiSummaryService aiSummaryService;

	@Mock
	private WhatsAppService whatsAppService;

	@Mock
	private PreferenceService preferenceService;

	@Test
	void collectsRanksPersistsSummarizesAndSendsDigest() {
		when(preferenceService.current()).thenReturn(PersonalPreference.defaults());
		var collected = new CollectedArticle(
				"rss",
				"article-1",
				"AI for Java backend teams",
				"https://example.com/ai-java",
				"Cloud backend summary",
				Instant.parse("2026-06-29T08:00:00Z"));
		when(collector.collect()).thenReturn(List.of(collected));
		when(articleRepository.existsByUrl(collected.url())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestItemRepository.save(any(DigestItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(aiSummaryService.summarize(anyList())).thenReturn("digest message");
		when(whatsAppService.sendText("digest message")).thenReturn(WhatsAppSendResult.sent());
		var service = new DailyDigestService(
				List.of(collector),
				articleRepository,
				digestRepository,
				digestItemRepository,
				new RankingService(new RankingProperties(List.of("ai", "java", "backend", "cloud"))),
				aiSummaryService,
				whatsAppService,
				preferenceService);

		var result = service.sendDailyDigest();

		assertThat(result.articleCount()).isEqualTo(1);
		assertThat(result.whatsAppStatus()).isEqualTo(WhatsAppSendStatus.SENT);
		verify(digestItemRepository).save(any(DigestItem.class));
	}

	@Test
	void limitsDigestItemsUsingPersonalPreferenceNewsLimit() {
		var preferences = PersonalPreference.defaults();
		preferences.apply(new PreferenceUpdateRequest(List.of(), List.of(), List.of(), 1, null, null));
		when(preferenceService.current()).thenReturn(preferences);
		var first = new CollectedArticle(
				"rss",
				"article-1",
				"AI for Java backend teams",
				"https://example.com/ai-java",
				"Cloud backend summary",
				Instant.parse("2026-06-29T08:00:00Z"));
		var second = new CollectedArticle(
				"rss",
				"article-2",
				"Redis backend patterns",
				"https://example.com/redis",
				"Backend summary",
				Instant.parse("2026-06-29T07:00:00Z"));
		when(collector.collect()).thenReturn(List.of(first, second));
		when(articleRepository.existsByUrl(first.url())).thenReturn(false);
		when(articleRepository.existsByUrl(second.url())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestItemRepository.save(any(DigestItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(aiSummaryService.summarize(anyList())).thenReturn("digest message");
		when(whatsAppService.sendText("digest message")).thenReturn(WhatsAppSendResult.sent());
		var service = new DailyDigestService(
				List.of(collector),
				articleRepository,
				digestRepository,
				digestItemRepository,
				new RankingService(new RankingProperties(List.of("ai", "java", "backend", "cloud"))),
				aiSummaryService,
				whatsAppService,
				preferenceService);

		service.sendDailyDigest();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RankedArticle>> captor = ArgumentCaptor.forClass(List.class);
		verify(aiSummaryService).summarize(captor.capture());
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().getFirst()).isInstanceOf(RankedArticle.class);
	}

	@Test
	void skipsArticlesAlreadyPersistedInHistory() {
		when(preferenceService.current()).thenReturn(PersonalPreference.defaults());
		var repeated = new CollectedArticle(
				"rss",
				"article-1",
				"AI for Java backend teams",
				"https://example.com/already-sent",
				"Cloud backend summary",
				Instant.parse("2026-06-29T08:00:00Z"));
		when(collector.collect()).thenReturn(List.of(repeated));
		when(articleRepository.existsByUrl(repeated.url())).thenReturn(true);
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(aiSummaryService.summarize(anyList())).thenReturn("no fresh news");
		when(whatsAppService.sendText("no fresh news")).thenReturn(WhatsAppSendResult.sent());
		var service = new DailyDigestService(
				List.of(collector),
				articleRepository,
				digestRepository,
				digestItemRepository,
				new RankingService(new RankingProperties(List.of("ai", "java", "backend", "cloud"))),
				aiSummaryService,
				whatsAppService,
				preferenceService);

		var result = service.sendDailyDigest();

		assertThat(result.articleCount()).isZero();
		verify(articleRepository, never()).save(any(Article.class));
		verify(digestItemRepository, never()).save(any(DigestItem.class));
	}

	@Test
	void sendsOnlyOneRepresentativeWhenDifferentUrlsDescribeTheSameEvent() {
		when(preferenceService.current()).thenReturn(PersonalPreference.defaults());
		var official = new CollectedArticle(
				"OpenAI",
				"official",
				"OpenAI launches Sol, Terra and Luna models",
				"https://openai.com/sol-terra-luna",
				"Official model launch",
				Instant.parse("2026-07-13T10:00:00Z"));
		var secondary = new CollectedArticle(
				"Tech blog",
				"secondary",
				"Sol, Terra and Luna unveiled by OpenAI",
				"https://example.com/openai-models",
				"Coverage of the same model launch",
				Instant.parse("2026-07-13T09:55:00Z"));
		when(collector.collect()).thenReturn(List.of(official, secondary));
		when(articleRepository.existsByUrl(official.url())).thenReturn(false);
		when(articleRepository.existsByUrl(secondary.url())).thenReturn(false);
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(digestItemRepository.save(any(DigestItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(aiSummaryService.summarize(anyList())).thenReturn("digest message");
		when(whatsAppService.sendText("digest message")).thenReturn(WhatsAppSendResult.sent());
		var service = new DailyDigestService(
				List.of(collector),
				articleRepository,
				digestRepository,
				digestItemRepository,
				new RankingService(new RankingProperties(
						List.of("ai"),
						List.of("openai.com"),
						List.of("openai", "sol", "terra", "luna"),
						List.of("launches", "unveiled"))),
				aiSummaryService,
				whatsAppService,
				preferenceService);

		var result = service.sendDailyDigest();

		assertThat(result.articleCount()).isEqualTo(1);
		verify(articleRepository, times(1)).save(any(Article.class));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<RankedArticle>> captor = ArgumentCaptor.forClass(List.class);
		verify(aiSummaryService).summarize(captor.capture());
		assertThat(captor.getValue()).singleElement().satisfies(ranked -> {
			assertThat(ranked.article().url()).isEqualTo(official.url());
			assertThat(ranked.eventKey()).isNotBlank();
		});
	}
}
