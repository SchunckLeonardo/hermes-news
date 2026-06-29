package com.hermesnews.ai;

import com.hermesnews.ranking.RankedArticle;
import java.util.List;

public interface AiSummaryService {

	String summarize(List<RankedArticle> articles);
}
