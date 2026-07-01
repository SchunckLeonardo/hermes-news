package com.hermesnews.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RssFeedDiscoveryTest {

	private final RssFeedDiscovery discovery = new RssFeedDiscovery();

	@Test
	void discoversAlternateRssLinksFromHtmlPages() {
		var html = """
				<html>
				  <head>
				    <link rel="alternate" type="application/rss+xml" title="RSS" href="/en/index.xml">
				  </head>
				</html>
				""";

		var feeds = discovery.discover("https://akitaonrails.com/en/", html);

		assertThat(feeds).containsExactly("https://akitaonrails.com/en/index.xml");
	}

	@Test
	void discoversRssAnchorsWhenAlternateLinkIsMissing() {
		var html = """
				<html>
				  <body>
				    <a href="/feed.xml">RSS</a>
				  </body>
				</html>
				""";

		var feeds = discovery.discover("https://example.com/blog/", html);

		assertThat(feeds).containsExactly("https://example.com/feed.xml");
	}
}
