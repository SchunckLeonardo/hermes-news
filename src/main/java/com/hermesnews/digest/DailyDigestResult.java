package com.hermesnews.digest;

import com.hermesnews.whatsapp.WhatsAppSendStatus;

public record DailyDigestResult(
		int articleCount,
		String message,
		WhatsAppSendStatus whatsAppStatus) {
}
