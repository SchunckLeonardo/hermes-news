package com.hermesnews.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hermesnews.ai.AiSummaryService;
import com.hermesnews.news.Article;
import com.hermesnews.news.ArticleRepository;
import com.hermesnews.news.CollectedArticle;
import com.hermesnews.news.NewsCollector;
import com.hermesnews.ranking.RankingProperties;
import com.hermesnews.ranking.RankingService;
import com.hermesnews.whatsapp.WhatsAppSendResult;
import com.hermesnews.whatsapp.WhatsAppSendStatus;
import com.hermesnews.whatsapp.WhatsAppService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

	@Test
	void collectsRanksPersistsSummarizesAndSendsDigest() {
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
				whatsAppService);

		var result = service.sendDailyDigest();

		assertThat(result.articleCount()).isEqualTo(1);
		assertThat(result.whatsAppStatus()).isEqualTo(WhatsAppSendStatus.SENT);
		verify(digestItemRepository).save(any(DigestItem.class));
	}
}
