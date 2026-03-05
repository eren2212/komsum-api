package com.ereniridere.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ereniridere.service.IEmailService;

@Service
public class EmailServiceImpl implements IEmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Override
	public void sendOtpEmail(String toEmail, String otp) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("komsum.app@gmail.com"); // Kendi mailini veya no-reply tarzı bir şey yaz
		message.setTo(toEmail);
		message.setSubject("Komşum Uygulaması - Şifre Sıfırlama Kodu");
		message.setText("Merhaba Komşu,\n\nŞifreni sıfırlamak için gereken 6 haneli doğrulama kodun: " + otp
				+ "\n\nBu kod 3 dakika boyunca geçerlidir. Lütfen kimseyle paylaşma!");

		mailSender.send(message);
	}

}
