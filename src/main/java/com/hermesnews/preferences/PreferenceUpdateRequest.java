package com.hermesnews.preferences;

import java.time.LocalTime;
import java.util.List;

public record PreferenceUpdateRequest(
		List<String> addThemes,
		List<String> removeThemes,
		List<String> sources,
		Integer newsLimit,
		LocalTime digestTime,
		String language) {
}
