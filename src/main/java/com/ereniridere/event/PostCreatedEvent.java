package com.ereniridere.event;

import com.ereniridere.entity.Post;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostCreatedEvent {
	private final Post post;
}
