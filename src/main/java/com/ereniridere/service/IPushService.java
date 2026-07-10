package com.ereniridere.service;

import java.util.List;
import java.util.Map;

/**
 * Push bildirimlerinin tek giriş noktası.
 *
 * Tüm gönderimler Expo Push Service'e ("ExponentPushToken[...]") gider;
 * Expo mesajı iOS'ta APNs'e, Android'de FCM'e iletir.
 */
public interface IPushService {

	void sendToToken(String token, String title, String body, Map<String, String> data);

	void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data);
}
