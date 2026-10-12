package com.ereniridere.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ereniridere.service.IEmailService;

@Service
public class EmailServiceImpl implements IEmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	private JavaMailSender mailSender;

	// Gmail SMTP, kimlik doğrulanan hesapla uyuşmayan bir From adresini
	// reddedebilir/spam'e atabilir; bu yüzden mail.username ile aynı olmalı.
	@Value("${spring.mail.username}")
	private String fromAddress;

	@Override
	public void sendOtpEmail(String toEmail, String otp) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(toEmail);
		message.setSubject("Komşum Uygulaması - Şifre Sıfırlama Kodu");
		message.setText("Merhaba Komşu,\n\nŞifreni sıfırlamak için gereken 6 haneli doğrulama kodun: " + otp
				+ "\n\nBu kod 3 dakika boyunca geçerlidir. Lütfen kimseyle paylaşma!");

		mailSender.send(message);
	}

	// Admin paneli yok: şikayet/engelleme olayları bu adrese e-postayla düşer ki
	// App Store'a verilen "24 saat içinde inceleme" taahhüdü yerine getirilebilsin.
	@Value("${application.moderation.email:}")
	private String moderationEmail;

	@Async("notificationExecutor")
	@Override
	public void sendModerationAlert(String subject, String body) {
		log.warn("[MODERASYON] {} — {}", subject, body.replace('\n', ' '));

		if (moderationEmail == null || moderationEmail.isBlank()) {
			return;
		}

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromAddress);
			message.setTo(moderationEmail);
			message.setSubject("[SelamKomşu Moderasyon] " + subject);
			message.setText(body);
			mailSender.send(message);
		} catch (Exception e) {
			// E-posta gönderilemedi diye şikayet/engelleme işlemi bozulmamalı.
			log.error("Moderasyon e-postası gönderilemedi: {}", e.getMessage());
		}
	}

}
