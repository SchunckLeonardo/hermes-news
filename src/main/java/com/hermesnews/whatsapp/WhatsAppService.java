package com.hermesnews.whatsapp;

public interface WhatsAppService {

	WhatsAppSendResult sendText(String message);

	WhatsAppSendResult sendTextTo(String recipient, String message);
}
