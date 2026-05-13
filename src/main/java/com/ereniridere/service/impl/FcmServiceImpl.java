package com.ereniridere.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IFcmService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

@Service
public class FcmServiceImpl implements IFcmService {

	private static final Logger log = LoggerFactory.getLogger(FcmServiceImpl.class);

	// FCM tek istekte en fazla 500 token kabul ediyor.
	private static final int FCM_MULTICAST_LIMIT = 500;

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
			log.warn("FirebaseMessaging hazır değil, multicast gönderilemedi.");
			return;
		}
		if (tokens == null || tokens.isEmpty()) {
			return;
		}

		List<String> distinctTokens = tokens.stream()
				.filter(t -> t != null && !t.isBlank())
				.distinct()
				.toList();

		for (int i = 0; i < distinctTokens.size(); i += FCM_MULTICAST_LIMIT) {
			List<String> chunk = distinctTokens.subList(i,
					Math.min(i + FCM_MULTICAST_LIMIT, distinctTokens.size()));

			MulticastMessage multicast = MulticastMessage.builder()
					.addAllTokens(chunk)
					.setNotification(Notification.builder().setTitle(title).setBody(body).build())
					.putAllData(safeData(data))
					.build();

			try {
				BatchResponse response = firebaseMessaging.sendEachForMulticast(multicast);
				handleBatchResponse(response, chunk);
			} catch (FirebaseMessagingException e) {
				log.error("FCM multicast tamamen başarısız oldu: {}", e.getMessagingErrorCode(), e);
			}
		}
	}

	private void cleanInvalidToken(MessagingErrorCode code, String token) {
		if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
			userRepository.clearFcmToken(token);
			log.info("Geçersiz FCM token temizlendi: {}", maskToken(token));
		}
	}

	private void handleBatchResponse(BatchResponse response, List<String> tokens) {
		if (response.getFailureCount() == 0) {
			return;
		}

		List<SendResponse> responses = response.getResponses();
		List<String> invalidTokens = new ArrayList<>();

		for (int i = 0; i < responses.size(); i++) {
			SendResponse r = responses.get(i);
			if (r.isSuccessful()) {
				continue;
			}
			FirebaseMessagingException ex = r.getException();
			if (ex == null) {
				continue;
			}
			MessagingErrorCode code = ex.getMessagingErrorCode();
			if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
				invalidTokens.add(tokens.get(i));
			} else {
				log.warn("FCM gönderim hatası (token={}): {}", maskToken(tokens.get(i)), code);
			}
		}

		invalidTokens.forEach(this::clearInvalidTokenTx);
	}

	private void clearInvalidTokenTx(String token) {
		userRepository.clearFcmToken(token);
		log.info("Geçersiz FCM token temizlendi (batch): {}", maskToken(token));
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
