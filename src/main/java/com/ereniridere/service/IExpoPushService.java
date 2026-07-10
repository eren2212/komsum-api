package com.ereniridere.service;

import java.util.List;
import java.util.Map;

/**
 * Expo Push Service (exp.host) üzerinden bildirim gönderimi.
 *
 * iOS istemcisi native Firebase SDK'sı taşımadığı için push token'ını Expo'dan
 * alır ("ExponentPushToken[...]" formatında). Bu token'lara gönderim FCM ile
 * değil, Expo'nun push API'si üzerinden yapılır; Expo mesajı APNs'e iletir.
 */
public interface IExpoPushService {

	/** Token Expo push token formatında mı? ("ExponentPushToken[...]") */
	boolean isExpoToken(String token);

	void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data);
}
