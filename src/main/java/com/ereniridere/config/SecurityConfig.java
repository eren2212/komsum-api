package com.ereniridere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ereniridere.security.JwtAuthenticationEntryPoint;
import com.ereniridere.security.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration // "Bu bir ayar dosyasıdır"
@EnableWebSecurity // "Web güvenlik kurallarını (Filtreleri) devreye sok!"
@RequiredArgsConstructor
public class SecurityConfig {

	// Kapıdaki güvenlik görevlimiz
	private final JwtAuthenticationFilter jwtAuthFilter;
	// AppConfig'den gelen kıyaslama cihazımız (Motor)
	private final AuthenticationProvider authenticationProvider;

	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				// 1. CSRF Korumasını Kapat: JWT kullandığımız için bu eski web (Session)
				// korumasına ihtiyacımız yok.
				.csrf(AbstractHttpConfigurer::disable)

				// 2. Yol (Endpoint) Kuralları: Kim nereye girebilir?
				.authorizeHttpRequests(auth -> auth
						// /api/auth/ ile başlayan yollara (Register, Login) HERKES GİREBİLİR (Biletsiz)
						.requestMatchers("/api/auth/**").permitAll()
						// Geri kalan BÜTÜN yollara (örn: /api/pets, /api/appointments) GİRİŞ
						// ZORUNLUDUR! (Biletli)
						.anyRequest().authenticated())

				// 3. Oturum Yönetimi (Stateless)
				.sessionManagement(session -> session
						// Sunucuda kimseyi hatırlama (Session tutma). Gelen her istekte bileti (JWT)
						// yeniden kontrol et!
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// 4. Aletleri Sisteme Tanıt
				.authenticationProvider(authenticationProvider)

				// 5. Filtre Sıralaması: Bizim yazdığımız jwtAuthFilter'ı, Spring'in standart
				// şifre kontrolünden (UsernamePasswordAuthenticationFilter) HEMEN ÖNCE
				// çalıştır.
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}