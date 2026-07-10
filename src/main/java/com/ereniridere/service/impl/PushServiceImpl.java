package com.ereniridere.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.service.IExpoPushService;
import com.ereniridere.service.IPushService;

/**
 * Push dağıtıcısı: tüm gönderimler Expo Push Service üzerinden yapılır.
 *
 * Hem iOS hem Android istemcisi Expo push token kaydeder ("ExponentPushToken[...]").
 * Expo bir relay'dir: iOS'a APNs, Android'e FCM üzerinden iletir — sunucunun
 * Firebase Admin SDK'ya ihtiyacı yoktur.
 *
 * Not: DB'de kalmış eski FCM device token'ları {@link IExpoPushService#sendToTokens}
 * içindeki önek filtresi tarafından sessizce atlanır. İlgili kullanıcı bir sonraki
 * login'de Expo token kaydettiğinde bildirim almaya devam eder.
 */
@Service
public class PushServiceImpl implements IPushService {

	@Autowired
	private IExpoPushService expoPushService;

	@Override
	public void sendToToken(String token, String title, String body, Map<String, String> data) {
		if (token == null || token.isBlank()) {
			return;
		}
		expoPushService.sendToTokens(List.of(token), title, body, data);
	}

	@Override
	public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
		if (tokens == null || tokens.isEmpty()) {
			return;
		}
		expoPushService.sendToTokens(tokens, title, body, data);
	}
}
