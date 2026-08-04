package com.ereniridere.security.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service // Spring'e "Bu bir servistir, hafızaya al" diyoruz
public class JwtService {

	// Token'ın ne işe yaradığını belirten claim. Bu claim olmadan access ve
	// refresh token yapısal olarak birbirinin aynısı olur; o zaman bir access
	// token refresh yerine kullanılıp sonsuza kadar yeni token üretilebilir.
	public static final String CLAIM_TOKEN_TYPE = "token_type";
	public static final String TOKEN_TYPE_ACCESS = "access";
	public static final String TOKEN_TYPE_REFRESH = "refresh";

	// 256-bit (32 byte) Base64 formatında gizli anahtar. Koda YAZILMAZ; ortam
	// değişkeninden (JWT_SECRET) gelir. application.yml'de varsayılan değeri de
	// yoktur — anahtar tanımlı değilse uygulama bilerek hiç açılmaz.
	@Value("${application.security.jwt.secret-key}")
	private String secretKey;

	// Access token ömrü (ms). Varsayılan 24 saat.
	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;

	// Refresh token ömrü (ms). Varsayılan 7 gün.
	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;

	// Anahtar hatası ilk login denemesinde değil, uygulama açılışında patlasın.
	// Base64 çözülemiyorsa ya da 256 bitten kısaysa (HS256'nın gerektirdiği
	// minimum) uygulama hiç ayağa kalkmaz.
	@PostConstruct
	void validateSecretKey() {
		byte[] keyBytes;
		try {
			keyBytes = Decoders.BASE64.decode(secretKey);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(
					"JWT_SECRET geçerli bir Base64 değeri değil. 'openssl rand -base64 32' ile üretin.", e);
		}
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET en az 256 bit (32 byte) olmalı, şu an " + keyBytes.length
					+ " byte. 'openssl rand -base64 32' ile üretin.");
		}
	}

	// 1. Token İçinden Kullanıcı Adını (Bizim projemizde Email olacak) Çekme
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	// 2. Basit Token Üretme (Ekstra bilgi olmadan)
	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	// 3. MÜFREDATTAKİ MADDE: Token İçerisine Map Gömmek (Ekstra Claim'ler)
	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>(extraClaims);
		claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

		return Jwts.builder().setClaims(claims) // İşte Map'i buraya gömüyoruz! (Roller, id vb. eklenebilir)
				.setSubject(userDetails.getUsername()) // Token kimin için üretildi?
				.setIssuedAt(new Date(System.currentTimeMillis())) // Üretim tarihi (Şu an)
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Bitiş tarihi (config'ten)
				.signWith(getSignInKey(), SignatureAlgorithm.HS256) // Gümrük mührü (Gizli anahtarımızla imzalıyoruz)
				.compact(); // Bütün bu bilgileri şifreli bir String'e çevir.
	}

	// 4. Kapıdaki Kontrol: Bu token geçerli mi? (Doğru kişiye mi ait ve süresi
	// dolmuş mu?)
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}

	public String generateRefreshToken(UserDetails userDetails) {
		return Jwts.builder().claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH).setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + refreshExpiration)) // config'ten (vars. 7 gün)
				.signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
	}

	/**
	 * Token'ın tipini döndürür ("access" / "refresh"). Tip claim'i taşımayan
	 * (bu değişiklikten önce üretilmiş) token'larda null döner.
	 */
	public String extractTokenType(String token) {
		return extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
	}

	/** Korumalı uçlarda yalnızca access token kabul edilir. */
	public boolean isAccessToken(String token) {
		return TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
	}

	/** /api/auth/refresh-token yalnızca refresh token kabul eder. */
	public boolean isRefreshToken(String token) {
		return TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
	}

	// Token'ın süresi dolmuş mu kontrolü
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	// Bitiş tarihini çekme
	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	// Token içinden istediğimiz bir parçayı (claim) almak için genel (generic) bir
	// metod
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	// Şifreli token'ı açıp içindeki yükleri (claims) okuduğumuz yer.
	// Eğer biri token'la oynamışsa burası hata fırlatır (Hacker koruması).
	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
	}

	// String olan şifremizi, JJWT kütüphanesinin anlayacağı kriptografik anahtara
	// dönüştürüyoruz
	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}