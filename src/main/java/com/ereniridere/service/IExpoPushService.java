package com.ereniridere.service;

import java.util.List;
import java.util.Map;

/**
 * Expo Push Service (exp.host) üzerinden bildirim gönderimi.
 *
 * Mobil istemci (iOS ve Android) push token'ını Expo'dan alır
 * ("ExponentPushToken[...]" formatında). Gönderim Expo'nun push API'si üzerinden
 * yapılır; Expo mesajı iOS'ta APNs'e, Android'de FCM'e iletir. Sunucunun kendi
 * Firebase kimlik bilgilerini taşımasına gerek yoktur.
 */
public interface IExpoPushService {

	/** Token Expo push token formatında mı? ("ExponentPushToken[...]") */
	boolean isExpoToken(String token);

	void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data);
}
