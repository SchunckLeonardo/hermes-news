package com.hermesnews.news;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class RssFeedParser {

	public List<CollectedArticle> parse(String sourceName, String xml) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);

			var builder = factory.newDocumentBuilder();
			var document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			var items = document.getElementsByTagName("item");
			var articles = new ArrayList<CollectedArticle>();
			for (int index = 0; index < items.getLength(); index++) {
				var item = items.item(index);
				if (item.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				var element = (Element) item;
				var title = text(element, "title");
				var link = text(element, "link");
				if (isBlank(title) || isBlank(link)) {
					continue;
				}
				var externalId = firstNonBlank(text(element, "guid"), link);
				articles.add(new CollectedArticle(
						sourceName,
						externalId,
						title,
						link,
						text(element, "description"),
						parsePublishedAt(text(element, "pubDate"))));
			}
			return articles;
		}
		catch (Exception exception) {
			throw new RssParsingException("Could not parse RSS feed", exception);
		}
	}

	private static String text(Element element, String tagName) {
		NodeList nodes = element.getElementsByTagName(tagName);
		if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
			return "";
		}
		return nodes.item(0).getTextContent().trim();
	}

	private static Instant parsePublishedAt(String value) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
		}
		catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private static String firstNonBlank(String preferred, String fallback) {
		return isBlank(preferred) ? fallback : preferred;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
