package com.ereniridere.security; // Kendi paket adını kontrol et

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.ereniridere.entity.RootEntity; // Senin o efsane zarf yapın!

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component // Spring'e "Bu bileşeni hafızana al" diyoruz
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {

		// 1. HTTP Statüsünü 401 (Unauthorized - Yetkisiz) olarak ayarlıyoruz.
		response.setStatus(HttpStatus.UNAUTHORIZED.value());

		// 2. Mobilde patlamasın diye cevabın tipini kesin bir dille JSON olarak
		// belirtiyoruz.
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// 3. SENİN YAZDIĞIN YAPIYI KULLANIYORUZ! Hata mesajını RootEntity ile
		// paketliyoruz.
		RootEntity<Object> errorResponse = RootEntity
				.error("Bu işleme yetkiniz yok veya biletinizin (Token) süresi dolmuş kanzi! Lütfen giriş yapın.");

		// 4. ObjectMapper (Jackson): Normalde Spring bu çeviri işini Controller'da
		// otomatik yapar.
		// Ama biz şu an Filtre katmanında olduğumuz için, Java objemizi (RootEntity)
		// JSON metnine kendi ellerimizle çevirip yolluyoruz.
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(response.getOutputStream(), errorResponse);
	}
}