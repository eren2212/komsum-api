package com.ereniridere.service;

import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.request.user.DtoForgotPassword;
import com.ereniridere.dto.request.user.DtoResetPassword;
import com.ereniridere.dto.response.DtoAuthenticationResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthenticationService {

	public DtoAuthenticationResponse register(DtoRegisterRequest request);

	public DtoAuthenticationResponse login(DtoLoginRequest request);

	public DtoAuthenticationResponse refreshToken(HttpServletRequest request);

	public void forgotPassword(DtoForgotPassword request);

	public void resetPassword(DtoResetPassword request);

}
