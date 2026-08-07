package com.ereniridere.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ereniridere.service.IEmailService;

@Service
public class EmailServiceImpl implements IEmailService {

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

}
