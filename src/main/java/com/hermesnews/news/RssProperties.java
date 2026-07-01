package com.hermesnews.news;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.rss")
public record RssProperties(List<String> feeds, DataSize maxResponseSize) {

	private static final DataSize DEFAULT_MAX_RESPONSE_SIZE = DataSize.ofMegabytes(2);

	@ConstructorBinding
	public RssProperties {
		if (maxResponseSize == null || maxResponseSize.toBytes() <= 0) {
			maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;
		}
	}

	public RssProperties(List<String> feeds) {
		this(feeds, DEFAULT_MAX_RESPONSE_SIZE);
	}

	int maxResponseBytes() {
		var bytes = maxResponseSize.toBytes();
		return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
	}
}
