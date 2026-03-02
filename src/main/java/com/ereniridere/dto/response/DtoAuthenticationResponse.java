package com.ereniridere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DtoAuthenticationResponse {
	// Başarılı işlem sonrası arayüze döneceğimiz tek şey bilet!
	private String token;
}