package com.ereniridere.controller;

import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.request.user.DtoForgotPassword;
import com.ereniridere.dto.request.user.DtoResetPassword;
import com.ereniridere.dto.response.DtoAuthenticationResponse;
import com.ereniridere.entity.RootEntity;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthenticationController {
	public RootEntity<DtoAuthenticationResponse> register(DtoRegisterRequest request);

	public RootEntity<DtoAuthenticationResponse> login(DtoLoginRequest request);

	public RootEntity<DtoAuthenticationResponse> refreshToken(HttpServletRequest request);

	public RootEntity<String> forgotPassword(DtoForgotPassword request);

	public RootEntity<String> resetPassword(DtoResetPassword request);
}
