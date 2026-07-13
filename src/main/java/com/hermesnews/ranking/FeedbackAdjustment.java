package com.hermesnews.ranking;

public record FeedbackAdjustment(int points, String reason) {

	public static FeedbackAdjustment none() {
		return new FeedbackAdjustment(0, "");
	}
}
