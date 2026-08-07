package com.ereniridere.controller.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ereniridere.dto.request.message.DtoSendMessage;
import com.ereniridere.dto.response.message.DtoMessage;
import com.ereniridere.entity.Role;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.MessageType;
import com.ereniridere.service.IChatService;
import com.ereniridere.service.IRealtimeChatService;
import com.ereniridere.service.impl.RateLimitingServiceImpl;

/**
 * ChatControllerImpl#sendMessage üzerine eklenen hız sınırlamasının (mesaj
 * spam'ini önlemek için) gerçekten devrede olduğunu doğrular.
 */
class ChatControllerImplTest {

	@Mock
	private IChatService chatService;

	@Mock
	private IRealtimeChatService realtimeChatService;

	private ChatControllerImpl controller;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		controller = new ChatControllerImpl();
		ReflectionTestUtils.setField(controller, "chatService", chatService);
		ReflectionTestUtils.setField(controller, "realtimeChatService", realtimeChatService);
		// Gerçek bucket4j davranışını (kapasite/refill) doğrulamak için sahte
		// yerine servisin kendisini kullanıyoruz.
		ReflectionTestUtils.setField(controller, "rateLimitingService", new RateLimitingServiceImpl());

		User currentUser = User.builder().id(11).email("user@example.com").password("x").role(Role.USER).build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities()));

		when(chatService.sendMessage(anyInt(), anyInt(), any(DtoSendMessage.class))).thenReturn(new DtoMessage());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void sendMessage_allowsUpToCapacityWithinWindow() {
		DtoSendMessage request = new DtoSendMessage();
		request.setContent("selam");

		// MESSAGE_CAPACITY = 10 (bkz. ChatControllerImpl)
		for (int i = 0; i < 10; i++) {
			assertThat(controller.sendMessage(1, request)).isNotNull();
		}
	}

	@Test
	void sendMessage_blocksAfterCapacityExceeded() {
		DtoSendMessage request = new DtoSendMessage();
		request.setContent("spam");

		for (int i = 0; i < 10; i++) {
			controller.sendMessage(1, request);
		}

		assertThatThrownBy(() -> controller.sendMessage(1, request)).isInstanceOf(BaseException.class)
				.satisfies(ex -> assertThat(((BaseException) ex).getMessageType()).isEqualTo(MessageType.TOO_MANY_REQUESTS));
	}
}
