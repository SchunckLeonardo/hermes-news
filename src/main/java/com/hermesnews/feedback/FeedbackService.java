package com.hermesnews.feedback;

import com.hermesnews.digest.DigestItem;
import com.hermesnews.digest.DigestItemRepository;
import com.hermesnews.digest.DigestRepository;
import com.hermesnews.news.CollectedArticle;
import com.hermesnews.ranking.FeedbackAdjustment;
import com.hermesnews.ranking.RankingFeedbackProvider;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService implements RankingFeedbackProvider {

	private static final int MAX_ADJUSTMENT = 12;
	private static final Set<String> IGNORED_WORDS = Set.of(
			"about", "after", "another", "como", "com", "developers", "from", "model", "news", "para", "sobre",
			"the", "uma", "with");

	private final DigestRepository digestRepository;
	private final DigestItemRepository digestItemRepository;
	private final ArticleFeedbackRepository feedbackRepository;

	public FeedbackService(
			DigestRepository digestRepository,
			DigestItemRepository digestItemRepository,
			ArticleFeedbackRepository feedbackRepository) {
		this.digestRepository = digestRepository;
		this.digestItemRepository = digestItemRepository;
		this.feedbackRepository = feedbackRepository;
	}

	@Transactional
	public FeedbackReceipt recordLatest(int rankOrder, FeedbackType type) {
		if (type == null) {
			throw new IllegalArgumentException("Feedback type is required");
		}
		var item = latestItem(rankOrder);
		var feedback = feedbackRepository.findByArticle(item.getArticle())
				.orElseGet(() -> new ArticleFeedback(item.getArticle(), type));
		feedback.changeTo(type);
		feedbackRepository.save(feedback);
		return new FeedbackReceipt(item.getArticle().getTitle(), type);
	}

	@Transactional
	public DigestItemExplanation explainLatest(int rankOrder) {
		var item = latestItem(rankOrder);
		return new DigestItemExplanation(
				item.getArticle().getTitle(),
				item.getRankScore(),
				item.getRankingExplanation());
	}

	@Override
	@Transactional
	public FeedbackAdjustment adjustmentFor(CollectedArticle candidate) {
		var points = 0;
		for (ArticleFeedback feedback : feedbackRepository.findTop100ByOrderByUpdatedAtDesc()) {
			var direction = feedback.getType() == FeedbackType.POSITIVE ? 1 : -1;
			var previous = feedback.getArticle();
			if (normalize(previous.getSourceName()).equals(normalize(candidate.sourceName()))) {
				points += direction * 3;
			}
			var previousTerms = terms(previous.getTitle() + " " + previous.getSummary());
			var candidateTerms = terms(candidate.title() + " " + candidate.summary());
			previousTerms.retainAll(candidateTerms);
			if (previousTerms.size() >= 2) {
				points += direction * 2;
			}
		}
		points = Math.max(-MAX_ADJUSTMENT, Math.min(MAX_ADJUSTMENT, points));
		if (points > 0) {
			return new FeedbackAdjustment(points, "Feedback positivo em noticias semelhantes");
		}
		if (points < 0) {
			return new FeedbackAdjustment(points, "Feedback negativo em noticias semelhantes");
		}
		return FeedbackAdjustment.none();
	}

	private DigestItem latestItem(int rankOrder) {
		if (rankOrder < 1) {
			throw new IllegalArgumentException("News position must be positive");
		}
		var digest = digestRepository.findFirstByOrderByGeneratedAtDesc()
				.orElseThrow(() -> new IllegalArgumentException("No digest is available"));
		return digestItemRepository.findByDigestAndRankOrder(digest, rankOrder)
				.orElseThrow(() -> new IllegalArgumentException("News position was not found in the latest digest"));
	}

	private static LinkedHashSet<String> terms(String value) {
		var terms = new LinkedHashSet<String>();
		for (String token : normalize(value).split("[^a-z0-9]+")) {
			if (token.length() >= 4 && !IGNORED_WORDS.contains(token)) {
				terms.add(token);
			}
		}
		return terms;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
