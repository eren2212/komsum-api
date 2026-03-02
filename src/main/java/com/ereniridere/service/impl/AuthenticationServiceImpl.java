package com.ereniridere.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
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

		return DtoAuthenticationResponse.builder().token(jwtToken).build();

	}

	@Override
	public DtoAuthenticationResponse login(DtoLoginRequest request) {

		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

		var jwtToken = jwtService.generateToken(user);

		return DtoAuthenticationResponse.builder().token(jwtToken).build();

	}

}
