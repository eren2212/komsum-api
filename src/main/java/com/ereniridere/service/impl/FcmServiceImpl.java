package com.ereniridere.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IFcmService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

@Service
public class FcmServiceImpl implements IFcmService {

	private static final Logger log = LoggerFactory.getLogger(FcmServiceImpl.class);

	@Autowired(required = false)
	private FirebaseMessaging firebaseMessaging;

	@Autowired
	private UserRepository userRepository;

	@Override
	public void sendToToken(String token, String title, String body, Map<String, String> data) {
		if (firebaseMessaging == null) {
			log.warn("FirebaseMessaging hazır değil, mesaj gönderilemedi.");
			return;
		}
		if (token == null || token.isBlank()) {
			return;
		}

		Message message = Message.builder()
				.setToken(token)
				.setNotification(Notification.builder().setTitle(title).setBody(body).build())
				.putAllData(safeData(data))
				.build();

		try {
			firebaseMessaging.send(message);
		} catch (FirebaseMessagingException e) {
			log.warn("FCM gönderim hatası (token={}): {}", maskToken(token), e.getMessagingErrorCode());
			cleanInvalidToken(e.getMessagingErrorCode(), token);
		}
	}

	@Override
	public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
		if (firebaseMessaging == null) {
			log.warn("FirebaseMessaging hazır değil, çoklu gönderim yapılamadı.");
			return;
		}
		if (tokens == null || tokens.isEmpty()) {
			log.info("FCM gönderilecek token yok, atlanıyor.");
			return;
		}

		List<String> distinctTokens = tokens.stream()
				.filter(t -> t != null && !t.isBlank())
				.distinct()
				.toList();

		log.info("FCM çoklu gönderim başlıyor — {} alıcı (title='{}')", distinctTokens.size(), title);

		int success = 0;
		int failure = 0;
		Map<String, String> safeData = safeData(data);

		for (String token : distinctTokens) {
			Message message = Message.builder()
					.setToken(token)
					.setNotification(Notification.builder().setTitle(title).setBody(body).build())
					.putAllData(safeData)
					.build();

			try {
				firebaseMessaging.send(message);
				success++;
			} catch (FirebaseMessagingException e) {
				failure++;
				log.warn("FCM gönderim hatası (token={}): {} - {}",
						maskToken(token), e.getMessagingErrorCode(), e.getMessage());
				cleanInvalidToken(e.getMessagingErrorCode(), token);
			}
		}

		log.info("FCM çoklu gönderim tamamlandı — başarılı: {}, başarısız: {}", success, failure);
	}

	private void cleanInvalidToken(MessagingErrorCode code, String token) {
		if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
			userRepository.clearFcmToken(token);
			log.info("Geçersiz FCM token temizlendi: {}", maskToken(token));
		}
	}

	private Map<String, String> safeData(Map<String, String> data) {
		if (data == null) {
			return new HashMap<>();
		}
		Map<String, String> safe = new HashMap<>();
		data.forEach((k, v) -> {
			if (v != null) {
				safe.put(k, v);
			}
		});
		return safe;
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 12) {
			return "***";
		}
		return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
	}
}
