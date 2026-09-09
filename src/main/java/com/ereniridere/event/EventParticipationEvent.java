package com.ereniridere.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Kullanıcı bir etkinliğe KATILDIĞINDA yayınlanır (ayrılmada yayınlanmaz).
 * Sadece id taşır — listener farklı transaction/thread'de fresh fetch yapar.
 */
@Getter
@RequiredArgsConstructor
public class EventParticipationEvent {
	private final Integer eventId;
	private final Integer userId;
}
