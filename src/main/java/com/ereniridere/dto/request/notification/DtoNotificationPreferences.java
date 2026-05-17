package com.ereniridere.dto.request.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoNotificationPreferences {

	@NotNull
	private Boolean postEnabled;

	@NotNull
	private Boolean eventEnabled;

	@NotNull
	private Boolean messageEnabled;
}
