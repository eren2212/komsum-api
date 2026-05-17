package com.ereniridere.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sadece eventId taşır — listener (farklı transaction/thread) içinde
 * fresh fetch yapılır. Entity geçirmek LAZY proxy çözüm hatası verir.
 */
@Getter
@RequiredArgsConstructor
public class EventCreatedEvent {
	private final Integer eventId;
}
