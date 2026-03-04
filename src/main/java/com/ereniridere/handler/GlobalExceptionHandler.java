package com.ereniridere.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.ereniridere.exception.BaseException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private List<String> addMapValue(List<String> list, String newValue) {
		list.add(newValue);
		return list;
	}

	@ExceptionHandler(value = { BaseException.class })
	public ResponseEntity<ApiError> handleBaseException(BaseException ex, WebRequest webRequest) {

		return ResponseEntity.badRequest().body(createApiError(ex.getMessage(), webRequest));
	}

	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			WebRequest request) {

		Map<String, List<String>> errorsMap = new HashMap<>();

		// burada errorların hepsini yakaladık
		for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
			String fieldString = ((FieldError) objectError).getField();// field alanını aldık burada

			if (errorsMap.containsKey(fieldString)) {
				// burada errorsMap.get(fieldString) ile o field a ait listeyi döndürürür.
				errorsMap.put(fieldString, addMapValue(errorsMap.get(fieldString), objectError.getDefaultMessage()));
			} else {

				errorsMap.put(fieldString, addMapValue(new ArrayList<>(), objectError.getDefaultMessage()));
			}
		}

		return ResponseEntity.badRequest().body(createApiError(errorsMap, request));

	}

	@ExceptionHandler(value = { BadCredentialsException.class })
	public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex, WebRequest webRequest) {

		ApiError<String> error = createApiError("E-posta veya şifre hatalı kanzi!", webRequest);
		error.setStatus(HttpStatus.UNAUTHORIZED.value());

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	public <E> ApiError<E> createApiError(E message, WebRequest request) {

		ApiError<E> apiError = new ApiError<>();
		apiError.setStatus(HttpStatus.BAD_REQUEST.value());

		Exception<E> exception = new Exception<>();
		exception.setHostName(getHostName());
		exception.setPath(request.getDescription(false).substring(5));
		exception.setCreateTime(new Date());
		exception.setMessage(message);

		apiError.setException(exception);

		return apiError;
	}

	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("Hostname almada hata var kanzi!!" + e.getMessage());
		}
		return null;
	}
}
