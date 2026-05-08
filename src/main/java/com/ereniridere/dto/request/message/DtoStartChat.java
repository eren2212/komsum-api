package com.ereniridere.dto.request.message;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoStartChat {

	@NotNull(message = "Kanzi kime mesaj atacağını seçmelisin!")
	private Integer targetUserId;

}