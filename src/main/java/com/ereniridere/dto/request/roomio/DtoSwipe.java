package com.ereniridere.dto.request.roomio;

import com.ereniridere.entity.enums.RoomioSwipeAction;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoSwipe {

	@NotNull(message = "Hangi kullanıcıyı değerlendirdiğini belirtmelisin")
	private Integer targetUserId;

	@NotNull(message = "LIKE ya da PASS seçmelisin")
	private RoomioSwipeAction action;
}
