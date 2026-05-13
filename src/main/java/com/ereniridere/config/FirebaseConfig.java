package com.ereniridere.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
public class FirebaseConfig {

	private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

	@Value("${firebase.service-account-base64:}")
	private String serviceAccountBase64;

	@Bean
	public FirebaseApp firebaseApp() throws IOException {
		if (FirebaseApp.getApps().stream().anyMatch(app -> FirebaseApp.DEFAULT_APP_NAME.equals(app.getName()))) {
			return FirebaseApp.getInstance();
		}

		if (serviceAccountBase64 == null || serviceAccountBase64.isBlank()) {
			log.warn(
					"FIREBASE_SERVICE_ACCOUNT_BASE64 boş. Firebase başlatılmadı — push bildirimler devre dışı kalacak.");
			return null;
		}

		byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(decoded)) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(stream))
					.build();
			log.info("Firebase Admin SDK başarıyla başlatıldı.");
			return FirebaseApp.initializeApp(options);
		}
	}

	@Bean
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		if (firebaseApp == null) {
			return null;
		}
		return FirebaseMessaging.getInstance(firebaseApp);
	}
}
