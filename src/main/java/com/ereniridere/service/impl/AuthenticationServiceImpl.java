package com.ereniridere.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.response.DtoAuthenticationResponse;
import com.ereniridere.entity.Role;
import com.ereniridere.entity.User;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.security.jwt.JwtService;
import com.ereniridere.service.IAuthenticationService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Override
	public DtoAuthenticationResponse register(DtoRegisterRequest request) {

		var user = User.builder().firstname(request.getFirstname()).lastname(request.getLastname())
				.email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).role(Role.USER)
				.build();

		userRepository.save(user);

		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);

		return DtoAuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).build();

	}

	@Override
	public DtoAuthenticationResponse login(DtoLoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);

		return DtoAuthenticationResponse.builder().accessToken(jwtToken).refreshToken(refreshToken).build();

	}

	public DtoAuthenticationResponse refreshToken(HttpServletRequest request) {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;

		// Header'da Bearer token yoksa hata fırlat (Bunu GlobalExceptionHandler'da
		// yakalayabilirsin)
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Refresh token bulunamadı!");
		}

		refreshToken = authHeader.substring(7);
		userEmail = jwtService.extractUsername(refreshToken);

		if (userEmail != null) {
			var user = this.userRepository.findByEmail(userEmail).orElseThrow();

			// Refresh Token sağlam mı ve süresi dolmamış mı kontrol et
			if (jwtService.isTokenValid(refreshToken, user)) {

				// Sağlamsa, adama şifre sormadan YEPYENİ bir Access Token üret!
				var accessToken = jwtService.generateToken(user);

				// Eski refresh token'ı kullanmaya devam etsin
				return DtoAuthenticationResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
			}
		}
		throw new RuntimeException("Geçersiz Refresh Token!");
	}

}
