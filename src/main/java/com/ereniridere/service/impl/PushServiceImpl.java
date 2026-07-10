package com.ereniridere.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.service.IExpoPushService;
import com.ereniridere.service.IFcmService;
import com.ereniridere.service.IPushService;

/**
 * Push dağıtıcısı: token formatına bakarak gönderimi doğru kanala yönlendirir.
 *
 * - iOS istemcisi Expo push token kaydeder ("ExponentPushToken[...]") → Expo Push API
 * - Android istemcisi FCM device token kaydeder → Firebase Admin (FCM)
 *
 * İki tür token da User.fcmToken alanında saklanır; ayrım burada yapılır.
 */
@Service
public class PushServiceImpl implements IPushService {

	@Autowired
	private IFcmService fcmService;

	@Autowired
	private IExpoPushService expoPushService;

	@Override
	public void sendToToken(String token, String title, String body, Map<String, String> data) {
		if (token == null || token.isBlank()) {
			return;
		}
		if (expoPushService.isExpoToken(token)) {
			expoPushService.sendToTokens(List.of(token), title, body, data);
		} else {
			fcmService.sendToToken(token, title, body, data);
		}
	}

	@Override
	public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
		if (tokens == null || tokens.isEmpty()) {
			return;
		}

		List<String> expoTokens = new ArrayList<>();
		List<String> fcmTokens = new ArrayList<>();
		for (String token : tokens) {
			if (token == null || token.isBlank()) {
				continue;
			}
			if (expoPushService.isExpoToken(token)) {
				expoTokens.add(token);
			} else {
				fcmTokens.add(token);
			}
		}

		if (!expoTokens.isEmpty()) {
			expoPushService.sendToTokens(expoTokens, title, body, data);
		}
		if (!fcmTokens.isEmpty()) {
			fcmService.sendToTokens(fcmTokens, title, body, data);
		}
	}
}
