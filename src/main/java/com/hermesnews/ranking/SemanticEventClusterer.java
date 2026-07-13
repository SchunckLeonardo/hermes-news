package com.hermesnews.ranking;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SemanticEventClusterer {

	private static final double MIN_JACCARD_SIMILARITY = 0.42;
	private static final Set<String> IGNORED_TERMS = Set.of(
			"about", "after", "and", "com", "como", "for", "from", "new", "noticia", "para", "por", "the", "uma",
			"with");

	public List<RankedArticle> cluster(List<RankedArticle> rankedArticles) {
		var clusters = new ArrayList<EventCluster>();
		for (RankedArticle ranked : rankedArticles) {
			var terms = terms(ranked.article().title());
			var existing = clusters.stream()
					.filter(cluster -> sameEvent(cluster.terms(), terms))
					.findFirst();
			if (existing.isPresent()) {
				existing.get().addSource();
				continue;
			}
			clusters.add(new EventCluster(ranked.withEventKey(eventKey(terms)), terms));
		}
		return clusters.stream().map(EventCluster::representative).toList();
	}

	private static boolean sameEvent(Set<String> first, Set<String> second) {
		if (first.isEmpty() || second.isEmpty()) {
			return false;
		}
		var intersection = new LinkedHashSet<>(first);
		intersection.retainAll(second);
		if (intersection.size() < 2) {
			return false;
		}
		var union = new LinkedHashSet<>(first);
		union.addAll(second);
		var jaccard = (double) intersection.size() / union.size();
		var smallerCoverage = (double) intersection.size() / Math.min(first.size(), second.size());
		return jaccard >= MIN_JACCARD_SIMILARITY || intersection.size() >= 3 && smallerCoverage >= 0.6;
	}

	private static LinkedHashSet<String> terms(String value) {
		var terms = new LinkedHashSet<String>();
		for (String token : normalize(value).split("[^a-z0-9]+")) {
			var canonical = canonicalTerm(token);
			if (canonical.length() >= 3 && !IGNORED_TERMS.contains(canonical)) {
				terms.add(canonical);
			}
		}
		return terms;
	}

	private static String canonicalTerm(String token) {
		return switch (token) {
			case "announce", "announced", "announces", "announcing", "introduces", "introducing", "launch",
					"launched", "launches", "release", "released", "releases", "unveil", "unveiled", "unveils" -> "launch";
			case "models" -> "model";
			default -> token;
		};
	}

	private static String eventKey(Set<String> terms) {
		return terms.stream()
				.sorted(Comparator.naturalOrder())
				.limit(12)
				.collect(java.util.stream.Collectors.joining("-"));
	}

	private static String normalize(String value) {
		var normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return normalized.toLowerCase(Locale.ROOT);
	}

	private static final class EventCluster {
		private final RankedArticle representative;
		private final Set<String> terms;
		private int sourceCount = 1;

		private EventCluster(RankedArticle representative, Set<String> terms) {
			this.representative = representative;
			this.terms = Set.copyOf(terms);
		}

		private RankedArticle representative() {
			return representative;
		}

		private Set<String> terms() {
			return terms;
		}

		private void addSource() {
			sourceCount++;
		}
	}
}
