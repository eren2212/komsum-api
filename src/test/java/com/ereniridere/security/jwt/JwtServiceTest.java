package com.ereniridere.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ereniridere.entity.Role;
import com.ereniridere.entity.User;

import io.jsonwebtoken.ExpiredJwtException;

class JwtServiceTest {

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService();
		// application.yml'deki JWT_SECRET'siz açılmama kuralına uymak için testte
		// sabit, geçerli (>=256 bit) bir Base64 anahtar veriyoruz.
		ReflectionTestUtils.setField(jwtService, "secretKey", "dGVzdC1zZWNyZXQta2V5LWZvci1qd3Qtc2VydmljZS10ZXN0LTEyMzQ1Ng==");
		ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
		ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120_000L);
	}

	private User buildUser(String email) {
		return User.builder().id(1).email(email).password("hashed").role(Role.USER).build();
	}

	@Test
	void generateToken_thenExtractUsername_matches() {
		User user = buildUser("ali@example.com");

		String token = jwtService.generateToken(user);

		assertThat(jwtService.extractUsername(token)).isEqualTo("ali@example.com");
		assertThat(jwtService.isTokenValid(token, user)).isTrue();
		assertThat(jwtService.isAccessToken(token)).isTrue();
		assertThat(jwtService.isRefreshToken(token)).isFalse();
	}

	@Test
	void generateRefreshToken_isMarkedAsRefreshType() {
		User user = buildUser("ayse@example.com");

		String refreshToken = jwtService.generateRefreshToken(user);

		assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
		assertThat(jwtService.isAccessToken(refreshToken)).isFalse();
	}

	@Test
	void isTokenValid_returnsFalse_whenUsernameDoesNotMatch() {
		User owner = buildUser("owner@example.com");
		User someoneElse = buildUser("other@example.com");

		String token = jwtService.generateToken(owner);

		assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
	}

	@Test
	void expiredToken_throwsWhenParsed() {
		// 0 negatif ömür vererek anında süresi dolmuş bir token üretiyoruz.
		ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
		User user = buildUser("expired@example.com");

		String token = jwtService.generateToken(user);

		assertThatThrownBy(() -> jwtService.isTokenValid(token, user)).isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void validateSecretKey_rejectsKeyShorterThan256Bits() {
		ReflectionTestUtils.setField(jwtService, "secretKey", "dG9vc2hvcnQ="); // "tooshort" base64, < 32 byte

		assertThatThrownBy(() -> jwtService.validateSecretKey()).isInstanceOf(IllegalStateException.class);
	}
}
