package com.hermesnews.whatsapp;

public record WhatsAppSendResult(WhatsAppSendStatus status, String detail) {

	public static WhatsAppSendResult sent() {
		return new WhatsAppSendResult(WhatsAppSendStatus.SENT, "sent");
	}

	public static WhatsAppSendResult skipped(String detail) {
		return new WhatsAppSendResult(WhatsAppSendStatus.SKIPPED, detail);
	}

	public static WhatsAppSendResult failed(String detail) {
		return new WhatsAppSendResult(WhatsAppSendStatus.FAILED, detail);
	}
}
