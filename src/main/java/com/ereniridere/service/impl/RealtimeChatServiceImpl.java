package com.ereniridere.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ereniridere.dto.response.message.DtoRealtimeMessage;
import com.ereniridere.service.IRealtimeChatService;

@Service
public class RealtimeChatServiceImpl implements IRealtimeChatService {

	private static final Logger log = LoggerFactory.getLogger(RealtimeChatServiceImpl.class);

	// SSE'de timeout yok (0L = sonsuz). Bağlantı kopuşunu heartbeat ve
	// onError/onCompletion callback'leri ile yönetiyoruz.
	private static final long NO_TIMEOUT = 0L;

	// userId -> o kullanıcının açık SSE bağlantıları (çoklu cihaz mümkün).
	private final Map<Integer, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

	@Override
	public SseEmitter subscribe(Integer userId) {
		SseEmitter emitter = new SseEmitter(NO_TIMEOUT);

		CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.computeIfAbsent(userId,
				k -> new CopyOnWriteArrayList<>());
		userEmitters.add(emitter);

		// Bağlantı kapanır/zaman aşımına uğrar/hata alırsa map'ten temizle.
		emitter.onCompletion(() -> removeEmitter(userId, emitter));
		emitter.onTimeout(() -> removeEmitter(userId, emitter));
		emitter.onError(e -> removeEmitter(userId, emitter));

		// İstemci bağlantının kurulduğunu anlasın diye ilk olayı hemen gönder.
		try {
			emitter.send(SseEmitter.event().name("connected").data("ok"));
		} catch (IOException e) {
			removeEmitter(userId, emitter);
		}

		return emitter;
	}

	@Override
	public void sendToUser(Integer userId, DtoRealtimeMessage payload) {
		List<SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null || userEmitters.isEmpty()) {
			return; // Kullanıcının açık bağlantısı yok (offline) — FCM zaten devrede.
		}

		for (SseEmitter emitter : userEmitters) {
			try {
				emitter.send(SseEmitter.event().name("message").data(payload));
			} catch (Exception e) {
				// Bağlantı ölmüş; listeden çıkar.
				removeEmitter(userId, emitter);
			}
		}
	}

	// nginx/idle timeout'a takılmamak ve ölü bağlantıları ayıklamak için
	// her açık emitter'a periyodik ping (SSE comment) gönderiyoruz.
	@Scheduled(fixedRate = 20000)
	public void heartbeat() {
		emitters.forEach((userId, userEmitters) -> {
			for (SseEmitter emitter : userEmitters) {
				try {
					emitter.send(SseEmitter.event().comment("ping"));
				} catch (Exception e) {
					removeEmitter(userId, emitter);
				}
			}
		});
	}

	private void removeEmitter(Integer userId, SseEmitter emitter) {
		CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters != null) {
			userEmitters.remove(emitter);
			if (userEmitters.isEmpty()) {
				emitters.remove(userId, userEmitters);
			}
		}
	}
}
