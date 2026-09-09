package com.ereniridere.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bir gönderiye yorum yazıldığında yayınlanır. Sadece id taşır — listener farklı
 * transaction/thread'de fresh fetch yapar.
 */
@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
	private final Integer commentId;
	private final Integer authorId;
}
