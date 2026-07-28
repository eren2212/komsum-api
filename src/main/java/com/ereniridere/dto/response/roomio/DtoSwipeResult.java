package com.ereniridere.dto.response.roomio;

import lombok.Data;

@Data
public class DtoSwipeResult {

	private boolean isMatch;

	private Integer matchId; // isMatch=false ise null

	// isMatch=true ise dolu — mobil ikinci bir istek atmadan direkt bu odaya
	// yönlenir (ChatServiceImpl.startChat zaten backend'de çağrılmış olur).
	private Integer chatRoomId;

	private DtoRoomioProfile matchedUser; // isMatch=false ise null
}
