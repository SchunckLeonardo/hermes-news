package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RssFeedParserTest {

	private final RssFeedParser parser = new RssFeedParser();

	@Test
	void parsesRssItemsIntoCollectedArticles() {
		var xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<rss version="2.0">
				  <channel>
				    <title>Tech Feed</title>
				    <item>
				      <guid>item-1</guid>
				      <title>Spring Boot with Java 21</title>
				      <link>https://example.com/spring-java-21</link>
				      <description>Backend development news</description>
				      <pubDate>Mon, 29 Jun 2026 08:00:00 GMT</pubDate>
				    </item>
				  </channel>
				</rss>
				""";

		var articles = parser.parse("Tech Feed", xml);

		assertThat(articles).hasSize(1);
		assertThat(articles.getFirst().title()).isEqualTo("Spring Boot with Java 21");
		assertThat(articles.getFirst().url()).isEqualTo("https://example.com/spring-java-21");
	}

	@Test
	void rejectsXmlDoctypeToAvoidXxe() {
		var xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
				<rss version="2.0">
				  <channel>
				    <item><title>&xxe;</title><link>https://example.com/xxe</link></item>
				  </channel>
				</rss>
				""";

		assertThatThrownBy(() -> parser.parse("Unsafe Feed", xml))
				.isInstanceOf(RssParsingException.class);
	}
}
