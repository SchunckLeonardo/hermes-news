package com.hermesnews.ranking;

import com.hermesnews.news.CollectedArticle;

public record RankedArticle(CollectedArticle article, int score) {
}
