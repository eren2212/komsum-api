package com.ereniridere.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.MessageType;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private List<String> addMapValue(List<String> list, String newValue) {
		list.add(newValue);
		return list;
	}

	@ExceptionHandler(value = { BaseException.class })
	public ResponseEntity<ApiError> handleBaseException(BaseException ex, WebRequest webRequest) {

		// Hata türü artık HTTP durum koduna yansıtılıyor: istemci ve izleme
		// araçları "kayıt yok" ile "çok hızlı istek" arasını ayırt edebilsin.
		HttpStatus status = resolveStatus(ex.getMessageType());

		ApiError<String> error = createApiError(ex.getMessage(), webRequest);
		error.setStatus(status.value());

		return ResponseEntity.status(status).body(error);
	}

	private HttpStatus resolveStatus(MessageType messageType) {
		if (messageType == null) {
			return HttpStatus.BAD_REQUEST;
		}
		return switch (messageType) {
		case NO_RECORD_EXIST, ROOMIO_PROFILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
		case RECORD_ALREADY_EXISTS, ROOMIO_PROFILE_ALREADY_EXISTS, ROOMIO_ALREADY_SWIPED -> HttpStatus.CONFLICT;
		case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
		case GENERAL_EXCEPTION -> HttpStatus.INTERNAL_SERVER_ERROR;
		default -> HttpStatus.BAD_REQUEST;
		};
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

	/**
	 * Yükleme boyutu Spring'in multipart limitini aşınca fırlar. Handler'ı
	 * olmadığında Spring'in kendi /error gövdesiyle 500 dönüyordu.
	 */
	@ExceptionHandler(value = { MaxUploadSizeExceededException.class })
	public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
			WebRequest webRequest) {

		ApiError<String> error = createApiError("Dosya çok büyük, daha küçük bir görsel seçin.", webRequest);
		error.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());

		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
	}

	/**
	 * Son savunma hattı: yakalanmamış her hata buraya düşer.
	 *
	 * Bu handler olmadan Spring'in varsayılan /error gövdesi devreye giriyordu;
	 * hem yanıt formatı diğerlerinden farklı oluyor hem de istisna türü/mesajı
	 * gibi iç detaylar istemciye sızabiliyordu. Detay artık yalnızca sunucu
	 * loguna yazılır, istemci genel bir mesaj görür.
	 */
	@ExceptionHandler(value = { java.lang.Exception.class })
	public ResponseEntity<ApiError> handleUnexpectedException(java.lang.Exception ex, WebRequest webRequest) {

		// Spring'in kendi web istisnaları (bilinmeyen yol → 404, yanlış HTTP
		// metodu → 405, okunamayan gövde → 400 ...) zaten doğru durum kodunu
		// taşır. Catch-all bunları da 500'e çevirmemeli.
		if (ex instanceof ErrorResponse errorResponse) {
			HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
			ApiError<String> known = createApiError(status.getReasonPhrase(), webRequest);
			known.setStatus(status.value());
			return ResponseEntity.status(status).body(known);
		}

		if (ex instanceof AccessDeniedException) {
			ApiError<String> denied = createApiError("Bu işlem için yetkiniz yok.", webRequest);
			denied.setStatus(HttpStatus.FORBIDDEN.value());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(denied);
		}

		// Buraya düşen her şey gerçekten beklenmeyen bir hatadır. Detay yalnızca
		// sunucu loguna yazılır; istemciye istisna türü/mesajı sızdırılmaz.
		log.error("Beklenmeyen hata: {}", webRequest.getDescription(false), ex);

		ApiError<String> error = createApiError("Beklenmeyen bir hata oluştu, lütfen tekrar deneyin.", webRequest);
		error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	public <E> ApiError<E> createApiError(E message, WebRequest request) {

		ApiError<E> apiError = new ApiError<>();
		apiError.setStatus(HttpStatus.BAD_REQUEST.value());

		Exception<E> exception = new Exception<>();
		exception.setPath(request.getDescription(false).substring(5));
		exception.setCreateTime(new Date());
		exception.setMessage(message);

		apiError.setException(exception);

		return apiError;
	}
}
