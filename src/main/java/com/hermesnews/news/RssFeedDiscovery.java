package com.hermesnews.news;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RssFeedDiscovery {

	private static final Pattern LINK_TAG = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern ANCHOR_TAG = Pattern.compile("<a\\b([^>]*)>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern ATTRIBUTE = Pattern.compile(
			"([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
			Pattern.CASE_INSENSITIVE);

	public List<String> discover(String pageUrl, String html) {
		if (isBlank(pageUrl) || isBlank(html)) {
			return List.of();
		}
		var feeds = new LinkedHashMap<String, Boolean>();
		discoverAlternateLinks(pageUrl, html, feeds);
		discoverAnchorLinks(pageUrl, html, feeds);
		return List.copyOf(feeds.keySet());
	}

	private static void discoverAlternateLinks(String pageUrl, String html, Map<String, Boolean> feeds) {
		var matcher = LINK_TAG.matcher(html);
		while (matcher.find()) {
			var attributes = attributes(matcher.group());
			var href = attributes.getOrDefault("href", "");
			var rel = attributes.getOrDefault("rel", "");
			var type = attributes.getOrDefault("type", "");
			if (contains(rel, "alternate") && (isFeedType(type) || isFeedHref(href))) {
				resolve(pageUrl, href, feeds);
			}
		}
	}

	private static void discoverAnchorLinks(String pageUrl, String html, Map<String, Boolean> feeds) {
		var matcher = ANCHOR_TAG.matcher(html);
		while (matcher.find()) {
			var attributes = attributes(matcher.group(1));
			var href = attributes.getOrDefault("href", "");
			var text = stripTags(matcher.group(2));
			if (isFeedHref(href) || contains(text, "rss") || contains(text, "atom") || contains(text, "feed")) {
				resolve(pageUrl, href, feeds);
			}
		}
	}

	private static Map<String, String> attributes(String tag) {
		var attributes = new LinkedHashMap<String, String>();
		var matcher = ATTRIBUTE.matcher(tag);
		while (matcher.find()) {
			var value = firstNonNull(matcher.group(3), matcher.group(4), matcher.group(5));
			attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), value == null ? "" : value.trim());
		}
		return attributes;
	}

	private static void resolve(String pageUrl, String href, Map<String, Boolean> feeds) {
		if (isBlank(href)) {
			return;
		}
		try {
			var resolved = URI.create(pageUrl).resolve(href.trim()).normalize().toString();
			feeds.putIfAbsent(NewsSourceService.normalizePublicHttpUrl(resolved), Boolean.TRUE);
		}
		catch (IllegalArgumentException ignored) {
			// Ignore malformed discovery candidates and keep trying other links.
		}
	}

	private static boolean isFeedType(String type) {
		return contains(type, "rss") || contains(type, "atom") || contains(type, "xml");
	}

	private static boolean isFeedHref(String href) {
		if (isBlank(href)) {
			return false;
		}
		var normalized = href.toLowerCase(Locale.ROOT).split("[?#]", 2)[0];
		return normalized.endsWith(".xml")
				|| normalized.endsWith(".rss")
				|| normalized.endsWith("/rss")
				|| normalized.endsWith("/atom")
				|| normalized.endsWith("/feed")
				|| normalized.contains("/rss/")
				|| normalized.contains("/atom/")
				|| normalized.contains("/feed/");
	}

	private static String stripTags(String value) {
		return value == null ? "" : value.replaceAll("<[^>]+>", " ").trim();
	}

	private static boolean contains(String value, String candidate) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(candidate);
	}

	private static String firstNonNull(String... values) {
		for (String value : values) {
			if (value != null) {
				return value;
			}
		}
		return "";
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
