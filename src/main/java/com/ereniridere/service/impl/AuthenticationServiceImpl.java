package com.ereniridere.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.request.user.DtoForgotPassword;
import com.ereniridere.dto.request.user.DtoResetPassword;
import com.ereniridere.dto.response.DtoAuthenticationResponse;
import com.ereniridere.entity.LegalDocument;
import com.ereniridere.entity.Role;
import com.ereniridere.entity.User;
import com.ereniridere.entity.UserConsent;
import com.ereniridere.entity.enums.LegalDocumentType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.LegalDocumentRepository;
import com.ereniridere.repository.UserConsentRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.INeighborhoodService;
import com.ereniridere.security.filter.JwtAuthenticationFilter;
import com.ereniridere.security.jwt.JwtService;
import com.ereniridere.service.IAuthenticationService;
import com.ereniridere.service.IEmailService;
import com.ereniridere.service.IRefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private INeighborhoodService neighborhoodService;

	@Autowired
	private IEmailService emailService;

	@Autowired
	private LegalDocumentRepository legalDocumentRepository;

	@Autowired
	private UserConsentRepository userConsentRepository;

	@Autowired
	private IRefreshTokenService refreshTokenService;

	// Şifre sıfırlama kodu üretimi için. java.util.Random tahmin edilebilirdir.
	private final SecureRandom secureRandom = new SecureRandom();

	AuthenticationServiceImpl(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Override
	public DtoAuthenticationResponse register(DtoRegisterRequest request) {

		// NOT: Kayıt akışında e-posta sızdırmasını (user enumeration) tamamen
		// kapatmak mümkün değil — kullanıcı zaten kayıtlıysa bunu söylemek
		// gerekiyor, yoksa hesabı olan biri neden kayıt olamadığını anlayamaz.
		// Otomatik e-posta taramasını Adım 2'de eklenen IP rate limiti
		// (20 istek/dk) pratikte kullanılamaz hâle getiriyor.
		if (userRepository.findByEmail(request.getEmail()).isPresent()) {
			throw new BaseException(
					new ErrorMessage(MessageType.RECORD_ALREADY_EXISTS, "Bu e-posta zaten kullanılıyor"));
		}

		// Onaylanan yasal metinleri doğrula: KVKK + Aydınlatma metninin ikisi de
		// kabul edilmiş olmalı.
		List<LegalDocument> acceptedDocuments = legalDocumentRepository
				.findAllById(request.getAcceptedLegalDocumentIds());
		Set<LegalDocumentType> acceptedTypes = EnumSet.noneOf(LegalDocumentType.class);
		for (LegalDocument doc : acceptedDocuments) {
			acceptedTypes.add(doc.getType());
		}
		if (!acceptedTypes.contains(LegalDocumentType.KVKK)
				|| !acceptedTypes.contains(LegalDocumentType.AYDINLATMA_METNI)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
					"Kayıt için KVKK Açık Rıza ve Aydınlatma metinlerinin ikisini de onaylamanız gerekiyor."));
		}

		var selectedNeighborhood = neighborhoodService.getOrCreate(request.getNeighborhoodId());

		var user = User.builder().firstname(request.getFirstname()).lastname(request.getLastname())
				.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).role(Role.USER)
				.neighborhood(selectedNeighborhood).build();

		userRepository.save(user);

		// Kabul edilen her metin için onay kaydı tut (KVKK uyumu / kabul kanıtı).
		for (LegalDocument doc : acceptedDocuments) {
			userConsentRepository.save(UserConsent.builder()
					.user(user)
					.documentType(doc.getType())
					.documentVersion(doc.getVersion())
					.build());
		}

		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		refreshTokenService.store(refreshToken, user);

		return DtoAuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).build();

	}

	@Override
	public DtoAuthenticationResponse login(DtoLoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		var user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı")));

		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		refreshTokenService.store(refreshToken, user);

		return DtoAuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).build();

	}

	@Override
	public DtoAuthenticationResponse refreshToken(HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Refresh token bulunamadı"));
		}

		refreshToken = authHeader.substring(7);

		// Burada SADECE refresh token kabul edilir. Aksi hâlde bir access token
		// refresh yerine kullanılıp süresiz olarak yeni access token üretilebilir
		// ve kısa ömürlü olmasının anlamı kalmazdı.
		if (!jwtService.isRefreshToken(refreshToken)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Geçersiz refresh token"));
		}

		userEmail = jwtService.extractUsername(refreshToken);

		if (userEmail != null) {
			var user = this.userRepository.findByEmail(userEmail)
					.orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı")));

			// Refresh Token sağlam mı ve süresi dolmamış mı kontrol et
			if (jwtService.isTokenValid(refreshToken, user)) {

				// Sunucu tarafındaki kaydı doğrula ve tüket. İptal edilmiş bir
				// token tekrar geldiyse burada tüm oturumlar kapatılır.
				refreshTokenService.consumeForRotation(refreshToken, user);

				// ROTATION: her yenilemede refresh token da yenilenir. Böylece
				// sızan bir token en fazla bir kez kullanılabilir ve meşru
				// istemci tekrar yenilemeye çalıştığında çalıntı tespit edilir.
				var accessToken = jwtService.generateToken(user);
				var newRefreshToken = jwtService.generateRefreshToken(user);
				refreshTokenService.store(newRefreshToken, user);

				return DtoAuthenticationResponse.builder().accessToken(accessToken).refreshToken(newRefreshToken)
						.build();
			}
		}
		throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Geçersiz refresh token"));
	}

	@Override
	public void logout(HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		// Çıkış her hâlükârda "başarılı" sayılır: istemci token'ı zaten siliyor,
		// sunucuda karşılığı yoksa yapacak bir şey yok. Sessizce yok saymak
		// aynı zamanda hangi token'ların geçerli olduğunu sızdırmaz.
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return;
		}

		refreshTokenService.revoke(authHeader.substring(7));
	}

	@Override
	public void forgotPassword(DtoForgotPassword request) {
		// 1. Kullanıcıyı bul
		Optional<User> optional = userRepository.findByEmail(request.getEmail());

		// E-posta kayıtlı değilse SESSİZCE çık. Controller her durumda aynı
		// başarı mesajını döndürür; aksi hâlde bu uç, hangi e-postaların sistemde
		// kayıtlı olduğunu tek tek sorgulamaya (user enumeration) izin verirdi.
		if (optional.isEmpty()) {
			return;
		}
		User user = optional.get();

		// 2. 6 Haneli Rastgele OTP Üret (Örn: 482910)
		// SecureRandom: java.util.Random tahmin edilebilir bir dizi üretir,
		// sıfırlama kodu gibi güvenlik değeri taşıyan sayılarda kullanılmaz.
		String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

		// 3. OTP'yi ve Son Kullanma Tarihini (Şu andan itibaren 3 DAKİKA) User objesine
		// kaydet
		user.setResetOtp(otp);
		user.setResetOtpExpiration(LocalDateTime.now().plusMinutes(3));
		userRepository.save(user);

		// 4. Postacıyı çağır ve e-postayı yolla!
		emailService.sendOtpEmail(user.getEmail(), otp);
	}

	@Override
	public void resetPassword(DtoResetPassword request) {

		// 1. Adamı bul
		Optional<User> optional = userRepository.findByEmail(request.getEmail());

		// Bilinmeyen e-posta için "kullanıcı bulunamadı" demek, bu ucu da bir
		// hesap sorgulama aracına çevirirdi. Kod hatalıymış gibi aynı mesaj döner.
		if (optional.isEmpty()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Girdiğiniz doğrulama kodu hatalı"));
		}
		User dbUser = optional.get();

		// 2. KOD DOĞRU MU? (sabit zamanlı karşılaştırma — eşleşen karakter
		// sayısına göre süre değişmesin)
		if (dbUser.getResetOtp() == null || request.getOtp() == null
				|| !MessageDigest.isEqual(dbUser.getResetOtp().getBytes(StandardCharsets.UTF_8),
						request.getOtp().getBytes(StandardCharsets.UTF_8))) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Girdiğiniz doğrulama kodu hatalı"));
		}

		// 3. KODUN SÜRESİ DOLMUŞ MU?
		if (dbUser.getResetOtpExpiration() == null || dbUser.getResetOtpExpiration().isBefore(LocalDateTime.now())) {
			throw new BaseException(new ErrorMessage(MessageType.COOLDOWN_ACTIVE,
					"Doğrulama kodunun süresi dolmuş. Lütfen tekrar kod isteyin."));
		}

		// 4. YENİ ŞİFRELER EŞLEŞİYOR MU?
		if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Yeni şifreler birbiriyle eşleşmiyor."));
		}

		dbUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

		// Tek kullanımlık olduğu için kodları temizliyoruz (Güvenlik!)
		dbUser.setResetOtp(null);
		dbUser.setResetOtpExpiration(null);

		userRepository.save(dbUser);

		// Şifre sıfırlamanın asıl amacı hesabı geri almaktır: hesabı ele geçiren
		// tarafın elindeki refresh token'lar geçersiz kılınmazsa şifre
		// değişmesine rağmen erişimi devam ederdi.
		refreshTokenService.revokeAllForUser(dbUser);
	}

}
