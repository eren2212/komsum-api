package com.ereniridere.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sadece postId taşır — listener (farklı transaction/thread) içinde
 * fresh fetch yapılır. Entity geçirmek LAZY proxy çözüm hatası verir.
 */
@Getter
@RequiredArgsConstructor
public class PostCreatedEvent {
	private final Integer postId;
}
