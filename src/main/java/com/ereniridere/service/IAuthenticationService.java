package com.ereniridere.service;

import com.ereniridere.dto.request.auth.DtoLoginRequest;
import com.ereniridere.dto.request.auth.DtoRegisterRequest;
import com.ereniridere.dto.response.DtoAuthenticationResponse;

public interface IAuthenticationService {

	public DtoAuthenticationResponse register(DtoRegisterRequest request);

	public DtoAuthenticationResponse login(DtoLoginRequest request);
}
