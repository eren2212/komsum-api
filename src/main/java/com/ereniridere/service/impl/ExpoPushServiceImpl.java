package com.ereniridere.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IExpoPushService;

/**
 * Expo Push API entegrasyonu (https://docs.expo.dev/push-notifications/sending-notifications/).
 *
 * POST https://exp.host/--/api/v2/push/send
 * İstek: [ { to, title, body, data, sound, priority }, ... ] (max 100 mesaj/istek)
 * Yanıt: { data: [ { status: "ok"|"error", message?, details?: { error? } } ] }
 * Yanıttaki sıra istekteki mesaj sırasıyla birebir eşleşir.
 */
@Service
public class ExpoPushServiceImpl implements IExpoPushService {

	private static final Logger log = LoggerFactory.getLogger(ExpoPushServiceImpl.class);

	private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
	private static final String EXPO_TOKEN_PREFIX = "ExponentPushToken[";
	private static final int CHUNK_SIZE = 100;

	@Autowired
	private UserRepository userRepository;

	private final RestClient restClient = RestClient.builder()
			.baseUrl(EXPO_PUSH_URL)
			.defaultHeader("Accept", "application/json")
			.build();

	@Override
	public boolean isExpoToken(String token) {
		return token != null && token.startsWith(EXPO_TOKEN_PREFIX);
	}

	@Override
	public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
		if (tokens == null || tokens.isEmpty()) {
			return;
		}

		List<String> distinctTokens = tokens.stream()
				.filter(this::isExpoToken)
				.distinct()
				.toList();
		if (distinctTokens.isEmpty()) {
			return;
		}

		log.info("Expo push gönderimi başlıyor — {} alıcı (title='{}')", distinctTokens.size(), title);

		for (int i = 0; i < distinctTokens.size(); i += CHUNK_SIZE) {
			List<String> chunk = distinctTokens.subList(i, Math.min(i + CHUNK_SIZE, distinctTokens.size()));
			sendChunk(chunk, title, body, data);
		}
	}

	private void sendChunk(List<String> chunk, String title, String body, Map<String, String> data) {
		List<Map<String, Object>> messages = new ArrayList<>(chunk.size());
		for (String token : chunk) {
			Map<String, Object> message = new HashMap<>();
			message.put("to", token);
			message.put("title", title);
			message.put("body", body);
			message.put("sound", "default");
			message.put("priority", "high");
			if (data != null && !data.isEmpty()) {
				message.put("data", data);
			}
			messages.add(message);
		}

		try {
			Map<String, Object> response = restClient.post()
					.contentType(MediaType.APPLICATION_JSON)
					.body(messages)
					.retrieve()
					.body(new ParameterizedTypeReference<Map<String, Object>>() {});
			handleResponse(response, chunk);
		} catch (Exception e) {
			log.error("Expo push gönderimi başarısız ({} token): {}", chunk.size(), e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void handleResponse(Map<String, Object> response, List<String> chunk) {
		if (response == null) {
			return;
		}

		// İstek seviyesinde hata (ör. tüm payload reddedildi)
		Object errors = response.get("errors");
		if (errors != null) {
			log.error("Expo push isteği reddedildi: {}", errors);
			return;
		}

		Object dataField = response.get("data");
		if (!(dataField instanceof List)) {
			return;
		}
		List<Map<String, Object>> tickets = (List<Map<String, Object>>) dataField;

		int failed = 0;
		for (int i = 0; i < tickets.size() && i < chunk.size(); i++) {
			Map<String, Object> ticket = tickets.get(i);
			if ("ok".equals(ticket.get("status"))) {
				continue;
			}
			failed++;
			String token = chunk.get(i);
			Object details = ticket.get("details");
			String errorCode = null;
			if (details instanceof Map) {
				Object err = ((Map<String, Object>) details).get("error");
				errorCode = err != null ? err.toString() : null;
			}
			log.warn("Expo push hatası (token={}): {} - {}", maskToken(token), errorCode, ticket.get("message"));

			// Cihaz artık kayıtlı değil → token'ı DB'den temizle (FCM UNREGISTERED muadili)
			if ("DeviceNotRegistered".equals(errorCode)) {
				userRepository.clearFcmToken(token);
				log.info("Geçersiz Expo push token temizlendi: {}", maskToken(token));
			}
		}

		log.info("Expo push tamamlandı — başarılı: {}, başarısız: {}", tickets.size() - failed, failed);
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 12) {
			return "***";
		}
		return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
	}
}
