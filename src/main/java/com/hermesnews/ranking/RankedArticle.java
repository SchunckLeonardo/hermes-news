package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;
import java.util.List;

public record RankedArticle(
		CollectedArticle article,
		int score,
		List<RankingReason> reasons,
		String eventKey) {

	public RankedArticle {
		reasons = reasons == null ? List.of() : List.copyOf(reasons);
		eventKey = eventKey == null ? "" : eventKey;
	}

	public RankedArticle(CollectedArticle article, int score) {
		this(article, score, List.of(), "");
	}

	public RankedArticle(CollectedArticle article, int score, List<RankingReason> reasons) {
		this(article, score, reasons, "");
	}

	public String explanation() {
		if (reasons.isEmpty()) {
			return "Sem sinais adicionais de ranking.";
		}
		return reasons.stream().map(RankingReason::formatted).collect(java.util.stream.Collectors.joining("; "));
	}

	public RankedArticle withEventKey(String value) {
		return new RankedArticle(article, score, reasons, value);
	}
}
