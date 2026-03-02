package com.ereniridere.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

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
	@JsonProperty("access_token") // Frontend'e giderken JSON anahtarı böyle görünsün
	private String accessToken;

	@JsonProperty("refresh_token")
	private String refreshToken;
}