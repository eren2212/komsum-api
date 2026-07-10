package com.ereniridere.dto.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Cihazın push token'ını kaydeder. Sınıf adı geçmişten kalma "Fcm" ismini taşır;
 * gelen değer artık Expo push token'dır ("ExponentPushToken[...]").
 */
@Data
public class DtoSaveFcmToken {

	@NotBlank(message = "Token boş olamaz")
	private String token;
}
