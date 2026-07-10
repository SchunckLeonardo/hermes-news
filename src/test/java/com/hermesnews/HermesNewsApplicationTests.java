package com.hermesnews;

import static org.assertj.core.api.Assertions.assertThat;

import com.hermesnews.news.RssProperties;
import com.hermesnews.ranking.RankingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HermesNewsApplicationTests {

	@Autowired
	private RssProperties rssProperties;

	@Autowired
	private RankingProperties rankingProperties;

	@Test
	void contextLoads() {
	}

	@Test
	void defaultConfigIncludesOfficialOpenAiFeedAndRankingSignals() {
		assertThat(rssProperties.feeds()).contains("https://openai.com/news/rss.xml");
		assertThat(rankingProperties.officialSources()).contains("openai.com", "spring.io", "kubernetes.io");
		assertThat(rankingProperties.priorityEntities()).contains("openai", "sol", "terra", "luna");
		assertThat(rankingProperties.launchKeywords()).contains("announces", "launch", "preview");
	}
}
