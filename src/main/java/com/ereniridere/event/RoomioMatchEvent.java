package com.ereniridere.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RoomioMatchEvent {
	private final Integer matchId;
	private final Integer userAId;
	private final Integer userBId;
}
