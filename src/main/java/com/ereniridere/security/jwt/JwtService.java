package com.ereniridere.security.jwt;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service // Spring'e "Bu bir servistir, hafızaya al" diyoruz
public class JwtService {

	// 256-bit (32 byte) Base64 formatında gizli anahtarımız.
	// Sektörde bu değer koda yazılmaz, .yml dosyasından gizlice okunur ama şimdilik
	// öğrenmek için buraya koyuyoruz.
	private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

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
		return Jwts.builder().setClaims(extraClaims) // İşte Map'i buraya gömüyoruz! (Roller, id vb. eklenebilir)
				.setSubject(userDetails.getUsername()) // Token kimin için üretildi?
				.setIssuedAt(new Date(System.currentTimeMillis())) // Üretim tarihi (Şu an)
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Bitiş tarihi (Örn: 24
																							// Saat)
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
		return Jwts.builder().setSubject(userDetails.getUsername()).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7)) // 7 GÜN
				.signWith(getSignInKey(), SignatureAlgorithm.HS256).compact();
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
		byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}