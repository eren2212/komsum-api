package com.ereniridere.event;

import com.ereniridere.entity.Message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MessageSentEvent {
	private final Message message;
	private final Integer recipientUserId;
}
