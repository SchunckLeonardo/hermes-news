package com.hermesnews.whatsapp;

public interface WhatsAppGateway {

	WhatsAppSendResult sendText(String message);

	WhatsAppSendResult sendTextTo(String recipient, String message);
}
