package com.ereniridere.security.filter;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ereniridere.security.jwt.JwtService; // JwtService'i import etmeyi unutma

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component // Spring'e "Bu bir bileşendir, projeye dahil et" diyoruz.
@RequiredArgsConstructor // Lombok harikası! Final olan değişkenler için otomatik constructor yazar.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	/**
	 * /api/auth/** uçları SecurityConfig'te zaten permitAll ve bir kısmı
	 * Authorization başlığında bilerek REFRESH token taşıyor
	 * (/refresh-token, /logout). Bu filtre yalnızca access token kabul ettiği
	 * için burada çalışsaydı o istekleri 401'e düşürürdü. Kimlik doğrulaması
	 * gerektirmeyen bu yolları tamamen atlıyoruz.
	 */
	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return request.getServletPath().startsWith("/api/auth/");
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		try {
			// 1. İstek başlığında (Header) "Authorization" var mı diye bakıyoruz.
			final String authHeader = request.getHeader("Authorization");
			final String jwt;
			final String userEmail;

			// Eğer Header yoksa veya sektör standardı olan "Bearer " kelimesiyle
			// başlamıyorsa...
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				filterChain.doFilter(request, response); // ...adamı sal, diğer filtrelere geçsin (Belki login sayfasına
															// gidiyordur).
				return;
			}

			// 2. "Bearer " kelimesi 7 karakterdir. Token'ı bu 7 karakterden sonrasını
			// keserek alıyoruz.
			jwt = authHeader.substring(7);

			// 2b. Korumalı uçlar SADECE access token kabul eder. Refresh token
			// çok daha uzun ömürlü olduğu için onunla normal API çağrısı
			// yapılabilseydi, sızan bir refresh token 7 gün boyunca tam erişim
			// demek olurdu. (Tip claim'i taşımayan eski token'lar da burada
			// elenir — anahtar rotasyonuyla zaten geçersizler.)
			if (!jwtService.isAccessToken(jwt)) {
				handleExceptionInFilter(response, "INVALID_TOKEN",
						"Bu istek için geçerli bir oturum anahtarı gerekiyor, lütfen tekrar giriş yap!");
				return;
			}

			// 3. Az önce yazdığımız makineyi (JwtService) kullanarak token içindeki email'i
			// cımbızlıyoruz.
			userEmail = jwtService.extractUsername(jwt);

			// 4. Eğer email bulduysak VE bu adam henüz sisteme "Giriş Yapmış" olarak
			// işaretlenmediyse...
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				// Supabase (Veritabanı)'den bu e-postaya sahip kullanıcıyı getir diyoruz.
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

				// 5. Token sahte mi, süresi geçmiş mi diye son kontrolü yapıyoruz.
				if (jwtService.isTokenValid(jwt, userDetails)) {

					// 6. Token sağlamsa, Spring Security için bir "Giriş Kartı"
					// (AuthenticationToken) oluşturuyoruz.
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());

					// İsteğin kimden, hangi cihazdan geldiği gibi ekstra detayları karta işliyoruz.
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					// 7. Mutlu Son: Adamı "VIP Odaya" (SecurityContext) alıyoruz. Artık sistem bu
					// adamı tanıyor!
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}

			// İşimiz bitti, isteği uygulamanın geri kalanına (Controller'lara) iletiyoruz.
			filterChain.doFilter(request, response);

		} catch (ExpiredJwtException ex) {
			// SENIOR DOKUNUŞU 1: Token süresi dolmuşsa!
			handleExceptionInFilter(response, "TOKEN_EXPIRED",
					"Kanzi oturumun süresi dolmuş, lütfen tekrar giriş yap!");
		} catch (SignatureException ex) {
			// SENIOR DOKUNUŞU 2: Adam token'ın sonundaki bir harfi değiştirip hacklemeye
			// çalışmışsa!
			handleExceptionInFilter(response, "INVALID_TOKEN", "Kanzi sahte token ile içeri giremezsin!");
		} catch (Exception ex) {
			// Beklenmedik başka bir JWT hatası
			handleExceptionInFilter(response, "UNAUTHORIZED", "Kimlik doğrulama hatası!");
		}

	}

	private void handleExceptionInFilter(HttpServletResponse response, String messageType, String message)
			throws IOException {
		// HTTP Statüsünü 401 (Yetkisiz) yapıyoruz
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		// Dışarıya JSON döneceğimizi ve Türkçe karakter desteklediğini söylüyoruz
		response.setContentType("application/json;charset=UTF-8");

		// Senin projende kullandığın RootEntity (veya BaseResponse) formatını manuel
		// olarak String'e döküyoruz
		// (Eğer istersen burayı ObjectMapper ile de yapabilirsin ama bu en hızlısıdır)
		String jsonResponse = String.format(
				"{\"data\":null,\"errorMessage\":{\"messageType\":\"%s\",\"message\":\"%s\"},\"result\":false}",
				messageType, message);

		// Cevabı kapıdan direkt postala ve işlemi kes!
		response.getWriter().write(jsonResponse);
	}
}