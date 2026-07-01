package com.hermesnews.news;

import java.time.Instant;
import java.util.UUID;

public record NewsSourceResponse(
		UUID id,
		String name,
		NewsSourceType type,
		String url,
		boolean enabled,
		Instant createdAt,
		Instant lastSuccessAt,
		Instant lastErrorAt,
		String lastErrorMessage,
		int consecutiveFailures,
		String status) {

	static NewsSourceResponse from(NewsSource source) {
		return new NewsSourceResponse(
				source.getId(),
				source.getName(),
				source.getType(),
				source.getUrl(),
				source.isEnabled(),
				source.getCreatedAt(),
				source.getLastSuccessAt(),
				source.getLastErrorAt(),
				source.getLastErrorMessage(),
				source.getConsecutiveFailures(),
				status(source));
	}

	private static String status(NewsSource source) {
		if (source.getConsecutiveFailures() > 0) {
			return "ERROR";
		}
		if (source.getLastSuccessAt() != null) {
			return "OK";
		}
		return "UNKNOWN";
	}
}
