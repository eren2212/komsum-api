package com.ereniridere.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.message.DtoSendMessage;
import com.ereniridere.dto.request.message.DtoStartChat;
import com.ereniridere.dto.response.message.DtoChatRoom;
import com.ereniridere.dto.response.message.DtoMessage;
import com.ereniridere.dto.response.message.DtoRealtimeMessage;
import com.ereniridere.entity.ChatRoom;
import com.ereniridere.entity.Message;
import com.ereniridere.entity.User;
import com.ereniridere.event.MessageSentEvent;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.ChatRoomRepository;
import com.ereniridere.repository.MessageRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IChatService;
import com.ereniridere.service.IRealtimeChatService;

@Service
public class ChatServiceImpl implements IChatService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private IRealtimeChatService realtimeChatService;

	@Override
	public DtoChatRoom startChat(Integer currentUserId, DtoStartChat request) {

		if (currentUserId.equals(request.getTargetUserId())) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Kanzi kendi kendine mesaj atamazsın!"));
		}

		User currentUser = userRepository.findById(currentUserId).get();
		User targetUser = userRepository.findById(request.getTargetUserId()).orElseThrow(() -> new BaseException(
				new ErrorMessage(MessageType.NO_RECORD_EXIST, "Mesaj atmak istediğin kişi bulunamadı!")));

		// Daha önce konuşmuşlar mı bakıyoruz
		Optional<ChatRoom> existingRoom = chatRoomRepository.findChatRoomBetweenUsers(currentUserId,
				targetUser.getId());

		ChatRoom room;
		if (existingRoom.isPresent()) {
			room = existingRoom.get(); // Konuşmuşlar, eski odayı ver
		} else {
			// Konuşmamışlar, yeni oda aç
			room = new ChatRoom();
			room.setUser1(currentUser);
			room.setUser2(targetUser);
			room = chatRoomRepository.save(room);
		}

		return convertToDtoChatRoom(room, currentUserId);
	}

	@Override
	public DtoMessage sendMessage(Integer currentUserId, Integer roomId, DtoSendMessage request) {

		ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Sohbet odası bulunamadı!")));

		// GÜVENLİK DUVARI: Bu adam bu odanın bir parçası mı? Dışarıdan biri ID sallayıp
		// odaya mesaj atamasın!
		if (!room.getUser1().getId().equals(currentUserId) && !room.getUser2().getId().equals(currentUserId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu odaya mesaj atma yetkin yok!"));
		}

		User sender = userRepository.findById(currentUserId).get();

		// 1. Mesajı Kaydet
		Message message = new Message();
		message.setChatRoom(room);
		message.setSender(sender);
		message.setContent(request.getContent());
		Message savedMessage = messageRepository.save(message);

		// 2. Odanın "Son Mesaj Tarihi"ni ve önizlemesini güncelle
		room.setLastMessageAt(LocalDateTime.now());
		String preview = request.getContent().length() > 200
				? request.getContent().substring(0, 197) + "..."
				: request.getContent();
		room.setLastMessageContent(preview);
		chatRoomRepository.save(room);

		// 2.1 Mesaj bildirimi async tetikle. Recipient = odadaki diğer kullanıcı.
		Integer recipientId = room.getUser1().getId().equals(currentUserId)
				? room.getUser2().getId()
				: room.getUser1().getId();
		eventPublisher.publishEvent(new MessageSentEvent(savedMessage, recipientId));

		// 2.2 Canlı teslim (SSE). Her iki katılımcıya da gönderiyoruz: alıcı anında
		// görsün, gönderenin diğer açık cihazları da senkron kalsın.
		DtoRealtimeMessage realtimePayload = new DtoRealtimeMessage();
		realtimePayload.setId(savedMessage.getId());
		realtimePayload.setChatRoomId(room.getId());
		realtimePayload.setSenderId(sender.getId());
		realtimePayload.setContent(savedMessage.getContent());
		realtimePayload.setCreatedAt(savedMessage.getCreatedAt());
		realtimeChatService.sendToUser(room.getUser1().getId(), realtimePayload);
		realtimeChatService.sendToUser(room.getUser2().getId(), realtimePayload);

		// 3. Ekrana dön
		DtoMessage dto = new DtoMessage();
		dto.setId(savedMessage.getId());
		dto.setSenderId(sender.getId());
		dto.setContent(savedMessage.getContent());
		dto.setCreatedAt(savedMessage.getCreatedAt());

		return dto;
	}

	@Override
	public Page<DtoMessage> getChatMessages(Integer currentUserId, Integer roomId, int pageNo, int pageSize) {

		ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Sohbet odası bulunamadı!")));

		// GÜVENLİK DUVARI: Bu mesajları okumaya yetkisi var mı?
		if (!room.getUser1().getId().equals(currentUserId) && !room.getUser2().getId().equals(currentUserId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Başkalarının mesajlarını okuyamazsın!"));
		}

		// Genelde mobilde sonsuz kaydırma yaparken eski mesajlara doğru inilir, bu
		// yüzden DESC (en yeni en üstte) çekeriz.
		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
		Page<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId, pageable);

		return messages.map(msg -> {
			DtoMessage dto = new DtoMessage();
			dto.setId(msg.getId());
			dto.setSenderId(msg.getSender().getId());
			dto.setContent(msg.getContent());
			dto.setCreatedAt(msg.getCreatedAt());
			return dto;
		});
	}

	@Override
	public Page<DtoChatRoom> getMyChatRooms(Integer currentUserId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<ChatRoom> myRooms = chatRoomRepository.getUserChatRooms(currentUserId, pageable);

		return myRooms.map(room -> convertToDtoChatRoom(room, currentUserId));
	}

	// 🚨 SENIOR DOKUNUŞU: Odanın içindeki "Diğer Adamı" bulan akıllı dönüşüm metodu
	// 🚨
	private DtoChatRoom convertToDtoChatRoom(ChatRoom room, Integer currentUserId) {
		DtoChatRoom dto = new DtoChatRoom();
		dto.setId(room.getId());
		dto.setLastMessageAt(room.getLastMessageAt());

		// Odanın içinde User1 ve User2 var. Karşımdaki kim?
		User otherUser;
		if (room.getUser1().getId().equals(currentUserId)) {
			otherUser = room.getUser2();
		} else {
			otherUser = room.getUser1();
		}

		dto.setOtherUserId(otherUser.getId());
		dto.setOtherUserFirstName(otherUser.getFirstname());
		dto.setOtherUserLastName(otherUser.getLastname());
		dto.setOtherUserAvatarUrl(otherUser.getAvatarUrl());
		dto.setLastMessageContent(room.getLastMessageContent());
		dto.setUnreadCount((int) messageRepository.countUnreadMessages(room.getId(), currentUserId));

		return dto;
	}

	@Override
	public void markAsRead(Integer currentUserId, Integer roomId) {
		ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Sohbet odası bulunamadı!")));

		if (!room.getUser1().getId().equals(currentUserId) && !room.getUser2().getId().equals(currentUserId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu odaya erişim yetkin yok!"));
		}

		messageRepository.markMessagesAsRead(roomId, currentUserId);
	}
}