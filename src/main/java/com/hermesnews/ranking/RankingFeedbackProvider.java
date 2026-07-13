package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;

@FunctionalInterface
public interface RankingFeedbackProvider {

	FeedbackAdjustment adjustmentFor(CollectedArticle article);
}
