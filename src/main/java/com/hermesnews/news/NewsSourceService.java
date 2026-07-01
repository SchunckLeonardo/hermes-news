package com.hermesnews.news;

import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class NewsSourceService {

	private final NewsSourceRepository repository;

	public NewsSourceService(NewsSourceRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public NewsSource addRssSource(String url) {
		var normalizedUrl = normalizePublicHttpUrl(url);
		return repository.findByUrl(normalizedUrl)
				.map(source -> {
					source.enable();
					return repository.save(source);
				})
				.orElseGet(() -> repository.save(new NewsSource(sourceName(normalizedUrl), NewsSourceType.RSS, normalizedUrl)));
	}

	@Transactional
	public NewsSource enableSource(String url) {
		var source = findExisting(url);
		source.enable();
		return repository.save(source);
	}

	@Transactional
	public NewsSource disableSource(String url) {
		var source = findExisting(url);
		source.disable();
		return repository.save(source);
	}

	public List<String> enabledRssUrls() {
		return repository.findAllByEnabledTrueAndType(NewsSourceType.RSS).stream()
				.map(NewsSource::getUrl)
				.toList();
	}

	private NewsSource findExisting(String url) {
		var normalizedUrl = normalizePublicHttpUrl(url);
		return repository.findByUrl(normalizedUrl)
				.orElseThrow(() -> new IllegalArgumentException("Source not found: " + normalizedUrl));
	}

	static String normalizePublicHttpUrl(String value) {
		try {
			var uri = new URI(value == null ? "" : value.trim()).normalize();
			var scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			var host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			if ((!scheme.equals("http") && !scheme.equals("https")) || host.isBlank() || isLocalOrPrivateHost(host)) {
				throw new IllegalArgumentException("Source URL must be a public http or https URL");
			}
			return uri.toString();
		}
		catch (URISyntaxException exception) {
			throw new IllegalArgumentException("Source URL must be a public http or https URL", exception);
		}
	}

	private static boolean isLocalOrPrivateHost(String host) {
		return host.equals("localhost")
				|| host.equals("0.0.0.0")
				|| host.equals("::1")
				|| host.startsWith("127.")
				|| host.startsWith("10.")
				|| host.startsWith("192.168.")
				|| isPrivate172(host)
				|| host.endsWith(".local");
	}

	private static boolean isPrivate172(String host) {
		if (!host.startsWith("172.")) {
			return false;
		}
		var parts = host.split("\\.");
		if (parts.length < 2) {
			return false;
		}
		try {
			var secondOctet = Integer.parseInt(parts[1]);
			return secondOctet >= 16 && secondOctet <= 31;
		}
		catch (NumberFormatException exception) {
			return false;
		}
	}

	private static String sourceName(String url) {
		var host = URI.create(url).getHost();
		if (host == null || host.isBlank()) {
			return "rss";
		}
		return host.startsWith("www.") ? host.substring(4) : host;
	}
}
