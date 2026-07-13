package com.hermesnews.ranking;

public record RankingReason(String code, int points, String description) {

	public String formatted() {
		var sign = points > 0 ? "+" : "";
		return description + " (" + sign + points + ")";
	}
}
