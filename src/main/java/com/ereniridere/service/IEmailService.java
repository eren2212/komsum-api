package com.ereniridere.service;

public interface IEmailService {
	public void sendOtpEmail(String toEmail, String otp);

	// Şikayet / engelleme / otomatik gizleme olaylarını moderasyon adresine bildirir.
	// MODERATION_EMAIL tanımlı değilse sadece log'a yazar.
	public void sendModerationAlert(String subject, String body);
}
