package com.ereniridere.service;

import java.util.List;
import java.util.Map;

/**
 * Push bildirimlerinin tek giriş noktası.
 *
 * Token formatına göre doğru kanala yönlendirir:
 * - "ExponentPushToken[...]" → Expo Push Service (iOS istemcisi)
 * - diğerleri → FCM (Android istemcisi, Firebase device token)
 */
public interface IPushService {

	void sendToToken(String token, String title, String body, Map<String, String> data);

	void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data);
}
