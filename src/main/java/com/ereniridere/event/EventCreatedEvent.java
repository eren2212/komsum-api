package com.ereniridere.event;

import com.ereniridere.entity.Event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EventCreatedEvent {
	private final Event event;
}
