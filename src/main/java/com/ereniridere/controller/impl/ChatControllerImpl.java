package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IChatController;
import com.ereniridere.dto.request.message.DtoSendMessage;
import com.ereniridere.dto.request.message.DtoStartChat;
import com.ereniridere.dto.response.message.DtoChatRoom;
import com.ereniridere.dto.response.message.DtoMessage;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chats")
public class ChatControllerImpl extends BaseController implements IChatController {

	@Autowired
	private IChatService chatService;

	// 1. Sohbet Başlat (Gidip adamın profiline tıkladığımızda çalışacak)
	@PostMapping("/start")
	@Override
	public RootEntity<DtoChatRoom> startChat(@Valid @RequestBody DtoStartChat request) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(chatService.startChat(userId, request));
	}

	// 2. Odaya Mesaj Gönder
	@PostMapping("/{roomId}/messages")
	@Override
	public RootEntity<DtoMessage> sendMessage(@PathVariable(value = "roomId") Integer roomId,
			@Valid @RequestBody DtoSendMessage request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(chatService.sendMessage(userId, roomId, request));
	}

	// 3. Odanın Mesajlarını Çek
	@GetMapping("/{roomId}/messages")
	@Override
	public RootEntity<Page<DtoMessage>> getChatMessages(@PathVariable(value = "roomId") Integer roomId,
			@RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(chatService.getChatMessages(userId, roomId, pageNo, pageSize));
	}

	// 4. Gelen Kutusu (Inbox - Benim Odalarım)
	@GetMapping("/inbox")
	@Override
	public RootEntity<Page<DtoChatRoom>> getMyChatRooms(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(chatService.getMyChatRooms(userId, pageNo, pageSize));
	}

	// 5. Odanın Mesajlarını Okundu İşaretle
	@PutMapping("/{roomId}/read")
	@Override
	public RootEntity<Void> markAsRead(@PathVariable(value = "roomId") Integer roomId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		chatService.markAsRead(currentUser.getId(), roomId);
		return ok(null);
	}
}