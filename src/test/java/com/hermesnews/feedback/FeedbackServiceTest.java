package com.hermesnews.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hermesnews.digest.Digest;
import com.hermesnews.digest.DigestItem;
import com.hermesnews.digest.DigestItemRepository;
import com.hermesnews.digest.DigestRepository;
import com.hermesnews.news.Article;
import com.hermesnews.news.CollectedArticle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

	@Mock
	private DigestRepository digestRepository;

	@Mock
	private DigestItemRepository digestItemRepository;

	@Mock
	private ArticleFeedbackRepository feedbackRepository;

	private Digest digest;
	private Article article;
	private DigestItem digestItem;
	private FeedbackService service;

	@BeforeEach
	void setUp() {
		digest = Digest.created("digest");
		article = Article.from(article("OpenAI launches a new model", "https://openai.com/model"), 42);
		digestItem = new DigestItem(
				digest,
				article,
				42,
				2,
				"Fonte oficial (+12); Publicada nas ultimas 24 horas (+8)",
				"openai-launches-new-model");
		service = new FeedbackService(digestRepository, digestItemRepository, feedbackRepository);
	}

	@Test
	void persistsFeedbackForAnItemFromTheLatestDigest() {
		when(digestRepository.findFirstByOrderByGeneratedAtDesc()).thenReturn(Optional.of(digest));
		when(digestItemRepository.findByDigestAndRankOrder(digest, 2)).thenReturn(Optional.of(digestItem));
		when(feedbackRepository.findByArticle(article)).thenReturn(Optional.empty());
		when(feedbackRepository.save(any(ArticleFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var receipt = service.recordLatest(2, FeedbackType.POSITIVE);

		assertThat(receipt.articleTitle()).isEqualTo("OpenAI launches a new model");
		assertThat(receipt.type()).isEqualTo(FeedbackType.POSITIVE);
		var captor = ArgumentCaptor.forClass(ArticleFeedback.class);
		verify(feedbackRepository).save(captor.capture());
		assertThat(captor.getValue().getArticle()).isEqualTo(article);
		assertThat(captor.getValue().getType()).isEqualTo(FeedbackType.POSITIVE);
	}

	@Test
	void returnsThePersistedRankingExplanationForTheLatestDigestItem() {
		when(digestRepository.findFirstByOrderByGeneratedAtDesc()).thenReturn(Optional.of(digest));
		when(digestItemRepository.findByDigestAndRankOrder(digest, 2)).thenReturn(Optional.of(digestItem));

		var explanation = service.explainLatest(2);

		assertThat(explanation.articleTitle()).isEqualTo("OpenAI launches a new model");
		assertThat(explanation.score()).isEqualTo(42);
		assertThat(explanation.explanation()).contains("Fonte oficial").contains("ultimas 24 horas");
	}

	@Test
	void turnsPriorFeedbackIntoABoundedRankingAdjustment() {
		var previous = Article.from(article("OpenAI model preview", "https://openai.com/preview"), 30);
		var positive = new ArticleFeedback(previous, FeedbackType.POSITIVE);
		when(feedbackRepository.findTop100ByOrderByUpdatedAtDesc()).thenReturn(List.of(positive));

		var adjustment = service.adjustmentFor(article("OpenAI releases another model", "https://openai.com/next"));

		assertThat(adjustment.points()).isPositive();
		assertThat(adjustment.reason()).contains("Feedback positivo");
	}

	private static CollectedArticle article(String title, String url) {
		return new CollectedArticle(
				"OpenAI",
				url,
				title,
				url,
				"AI model for developers",
				Instant.parse("2026-07-13T10:00:00Z"));
	}
}
