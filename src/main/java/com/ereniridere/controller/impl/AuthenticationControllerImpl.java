package com.ereniridere.controller.impl;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IAuthenticationController;
import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.request.user.DtoForgotPassword;
import com.ereniridere.dto.request.user.DtoResetPassword;
import com.ereniridere.dto.response.DtoAuthenticationResponse;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IAuthenticationService;
import com.ereniridere.service.IRateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationControllerImpl extends BaseController implements IAuthenticationController {

	// ─── Rate limit ayarları ────────────────────────────────────────────────
	// Auth uçları token gerektirmediği için brute-force ve spam'e açıktır.
	// İki katman uygulanır: IP başına genel bir tavan + hedeflenen hesabı
	// koruyan e-posta başına daha sıkı bir limit.
	//
	// IP limiti mobil operatör NAT'ı yüzünden bilerek gevşek tutuldu; aynı
	// public IP'nin arkasında çok sayıda gerçek kullanıcı olabiliyor.
	private static final int IP_CAPACITY = 20;
	private static final Duration IP_PERIOD = Duration.ofMinutes(1);

	// Tek bir hesaba yönelik şifre denemesi.
	private static final int LOGIN_EMAIL_CAPACITY = 5;
	private static final Duration LOGIN_EMAIL_PERIOD = Duration.ofMinutes(1);

	// Şifre sıfırlama e-postası spam'ini engeller (posta kutusunu doldurma).
	private static final int FORGOT_EMAIL_CAPACITY = 3;
	private static final Duration FORGOT_EMAIL_PERIOD = Duration.ofHours(1);

	// 6 haneli OTP'nin kaba kuvvetle denenmesini engeller.
	private static final int RESET_EMAIL_CAPACITY = 5;
	private static final Duration RESET_EMAIL_PERIOD = Duration.ofMinutes(1);

	private final UserRepository userRepository;

	@Autowired
	private IAuthenticationService authenticationService;

	@Autowired
	private IRateLimitingService rateLimitingService;

	// İstek başına scoped proxy — arayüz imzalarını bozmadan her metotta
	// çağıranın IP'sine erişebilmek için.
	@Autowired
	private HttpServletRequest httpServletRequest;

	AuthenticationControllerImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@PostMapping(path = "/register")
	@Override
	public RootEntity<DtoAuthenticationResponse> register(@Valid @RequestBody DtoRegisterRequest request) {

		checkIpLimit();

		return ok(authenticationService.register(request));
	}

	@PostMapping(path = "/login")
	@Override
	public RootEntity<DtoAuthenticationResponse> login(@Valid @RequestBody DtoLoginRequest request) {

		checkIpLimit();
		consume("login:" + normalizeEmail(request.getEmail()), LOGIN_EMAIL_CAPACITY, LOGIN_EMAIL_PERIOD);

		return ok(authenticationService.login(request));
	}

	@PostMapping(path = "/refresh-token")
	@Override
	public RootEntity<DtoAuthenticationResponse> refreshToken(HttpServletRequest request) {

		// Bu uç bilerek IP limitinin dışında: geçerli bir refresh token
		// gerektiriyor ve aynı NAT arkasındaki cihazlar eşzamanlı yenileme
		// yaptığında gerçek kullanıcıları kilitlememeli.
		return ok(authenticationService.refreshToken(request));
	}

	@PostMapping("/forgot-password")
	public RootEntity<String> forgotPassword(@Valid @RequestBody DtoForgotPassword request) {

		checkIpLimit();
		consume("forgot:" + normalizeEmail(request.getEmail()), FORGOT_EMAIL_CAPACITY, FORGOT_EMAIL_PERIOD);

		authenticationService.forgotPassword(request);

		return ok("Şifre sıfırlama kodu e-postanıza gönderildi!");
	}

	@PostMapping("/reset-password")
	@Override
	public RootEntity<String> resetPassword(@Valid @RequestBody DtoResetPassword request) {

		checkIpLimit();
		consume("reset:" + normalizeEmail(request.getEmail()), RESET_EMAIL_CAPACITY, RESET_EMAIL_PERIOD);

		authenticationService.resetPassword(request);
		return ok("Şifre sıfırlama işlemi başarıyla gerçekleştirildi!");
	}

	// ─── Rate limit yardımcıları ────────────────────────────────────────────

	private void checkIpLimit() {
		consume("ip:" + resolveClientIp(), IP_CAPACITY, IP_PERIOD);
	}

	/** Kovadan bir jeton düşer; kova boşsa isteği reddeder. */
	private void consume(String key, int capacity, Duration period) {
		Bucket bucket = rateLimitingService.resolveBucket(key, capacity, period);
		if (!bucket.tryConsume(1)) {
			throw new BaseException(new ErrorMessage(MessageType.TOO_MANY_REQUESTS, null));
		}
	}

	/**
	 * Çağıranın IP'si. Uygulama ters proxy (nginx/Traefik) arkasında çalıştığı
	 * için önce X-Forwarded-For'un ilk değerine bakılır; proxy yoksa doğrudan
	 * bağlantı adresi kullanılır.
	 */
	private String resolveClientIp() {
		String forwarded = httpServletRequest.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return httpServletRequest.getRemoteAddr();
	}

	/** Aynı hesabın büyük/küçük harf varyasyonlarıyla limiti atlatmasını önler. */
	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

}
