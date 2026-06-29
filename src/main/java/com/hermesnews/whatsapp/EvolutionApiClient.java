package com.hermesnews.whatsapp;

public interface EvolutionApiClient {

	WhatsAppSendResult sendText(EvolutionProperties properties, String message);
}
