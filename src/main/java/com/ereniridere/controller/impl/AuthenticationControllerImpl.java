package com.ereniridere.controller.impl;

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
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IAuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationControllerImpl extends BaseController implements IAuthenticationController {

	private final UserRepository userRepository;

	@Autowired
	private IAuthenticationService authenticationService;

	AuthenticationControllerImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@PostMapping(path = "/register")
	@Override
	public RootEntity<DtoAuthenticationResponse> register(@Valid @RequestBody DtoRegisterRequest request) {

		return ok(authenticationService.register(request));
	}

	@PostMapping(path = "/login")
	@Override
	public RootEntity<DtoAuthenticationResponse> login(@Valid @RequestBody DtoLoginRequest request) {

		return ok(authenticationService.login(request));
	}

	@PostMapping(path = "/refresh-token")
	@Override
	public RootEntity<DtoAuthenticationResponse> refreshToken(HttpServletRequest request) {

		return ok(authenticationService.refreshToken(request));
	}

	@PostMapping("/forgot-password")
	public RootEntity<String> forgotPassword(@Valid @RequestBody DtoForgotPassword request) {

		authenticationService.forgotPassword(request);

		return ok("Şifre sıfırlama kodu e-postanıza gönderildi!");
	}

	@PostMapping("/reset-password")
	@Override
	public RootEntity<String> resetPassword(@Valid @RequestBody DtoResetPassword request) {

		authenticationService.resetPassword(request);
		return ok("Şifre sıfırlama işlemi başarıyla gerçekleştirildi!");
	}

}
