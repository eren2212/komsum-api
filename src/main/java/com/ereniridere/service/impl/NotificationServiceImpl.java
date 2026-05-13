package com.ereniridere.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.dto.response.notification.DtoNotification;
import com.ereniridere.entity.ChatRoom;
import com.ereniridere.entity.Event;
import com.ereniridere.entity.Message;
import com.ereniridere.entity.Notification;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.NotificationType;
import com.ereniridere.entity.enums.RelatedEntityType;
import com.ereniridere.event.EventCreatedEvent;
import com.ereniridere.event.MessageSentEvent;
import com.ereniridere.event.PostCreatedEvent;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.NotificationRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IFcmService;
import com.ereniridere.service.INotificationService;

@Service
public class NotificationServiceImpl implements INotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

	private static final int BODY_PREVIEW_MAX = 200;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IFcmService fcmService;

	// ============================
	// EVENT LISTENERS (ASYNC)
	// ============================

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handlePostCreated(PostCreatedEvent event) {
		try {
			Post post = event.getPost();
			User author = post.getAuthor();

			if (author == null || author.getNeighborhood() == null) {
				return;
			}

			String district = author.getNeighborhood().getDistrict();
			List<User> recipients = userRepository.findByDistrictExcludingUser(district, author.getId());

			if (recipients.isEmpty()) {
				return;
			}

			String title = author.getFirstname() + " bir gönderi paylaştı";
			String body = preview(post.getContent());

			Map<String, String> data = baseData(NotificationType.NEW_POST,
					RelatedEntityType.POST, post.getId().longValue(), author);

			List<Notification> notifications = new ArrayList<>();
			List<String> tokens = new ArrayList<>();

			for (User r : recipients) {
				notifications.add(buildNotification(r, author, NotificationType.NEW_POST, title, body,
						RelatedEntityType.POST, post.getId().longValue()));
				if (r.getFcmToken() != null && !r.getFcmToken().isBlank()) {
					tokens.add(r.getFcmToken());
				}
			}

			notificationRepository.saveAll(notifications);
			fcmService.sendToTokens(tokens, title, body, data);
		} catch (Exception e) {
			log.error("Post bildirimi gönderilemedi", e);
		}
	}

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handleEventCreated(EventCreatedEvent event) {
		try {
			Event ev = event.getEvent();
			User author = ev.getAuthor();

			if (author == null || author.getNeighborhood() == null) {
				return;
			}

			String district = author.getNeighborhood().getDistrict();
			List<User> recipients = userRepository.findByDistrictExcludingUser(district, author.getId());

			if (recipients.isEmpty()) {
				return;
			}

			String title = author.getFirstname() + " bir etkinlik oluşturdu";
			String body = preview(ev.getTitle());

			Map<String, String> data = baseData(NotificationType.NEW_EVENT,
					RelatedEntityType.EVENT, ev.getId().longValue(), author);

			List<Notification> notifications = new ArrayList<>();
			List<String> tokens = new ArrayList<>();

			for (User r : recipients) {
				notifications.add(buildNotification(r, author, NotificationType.NEW_EVENT, title, body,
						RelatedEntityType.EVENT, ev.getId().longValue()));
				if (r.getFcmToken() != null && !r.getFcmToken().isBlank()) {
					tokens.add(r.getFcmToken());
				}
			}

			notificationRepository.saveAll(notifications);
			fcmService.sendToTokens(tokens, title, body, data);
		} catch (Exception e) {
			log.error("Etkinlik bildirimi gönderilemedi", e);
		}
	}

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handleMessageSent(MessageSentEvent event) {
		try {
			Message message = event.getMessage();
			Integer recipientId = event.getRecipientUserId();

			Optional<User> recipientOpt = userRepository.findById(recipientId);
			if (recipientOpt.isEmpty()) {
				return;
			}
			User recipient = recipientOpt.get();
			User sender = message.getSender();
			ChatRoom room = message.getChatRoom();

			String title = sender.getFirstname() + " mesaj gönderdi";
			String body = preview(message.getContent());

			Map<String, String> data = baseData(NotificationType.NEW_MESSAGE,
					RelatedEntityType.CHAT_ROOM, room.getId().longValue(), sender);

			Notification notification = buildNotification(recipient, sender, NotificationType.NEW_MESSAGE,
					title, body, RelatedEntityType.CHAT_ROOM, room.getId().longValue());
			notificationRepository.save(notification);

			if (recipient.getFcmToken() != null && !recipient.getFcmToken().isBlank()) {
				fcmService.sendToToken(recipient.getFcmToken(), title, body, data);
			}
		} catch (Exception e) {
			log.error("Mesaj bildirimi gönderilemedi", e);
		}
	}

	// ============================
	// USER-FACING APIs
	// ============================

	@Override
	public Page<DtoNotification> getMyNotifications(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Notification> page = notificationRepository.findByRecipientId(userId, pageable);
		return page.map(this::toDto);
	}

	@Override
	public long getUnreadCount(Integer userId) {
		return notificationRepository.countUnreadByRecipientId(userId);
	}

	@Override
	@Transactional
	public void markAsRead(Integer userId, Long notificationId) {
		int updated = notificationRepository.markAsRead(notificationId, userId);
		if (updated == 0) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Bildirim bulunamadı"));
		}
	}

	@Override
	@Transactional
	public void markAllAsRead(Integer userId) {
		notificationRepository.markAllAsRead(userId);
	}

	@Override
	@Transactional
	public void saveFcmToken(Integer userId, String token) {
		User user = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));
		user.setFcmToken(token);
		userRepository.save(user);
	}

	@Override
	@Transactional
	public void clearFcmToken(Integer userId) {
		User user = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));
		user.setFcmToken(null);
		userRepository.save(user);
	}

	// ============================
	// HELPERS
	// ============================

	private Notification buildNotification(User recipient, User actor, NotificationType type,
			String title, String body, RelatedEntityType relatedEntityType, Long relatedEntityId) {
		return Notification.builder()
				.recipient(recipient)
				.actor(actor)
				.type(type)
				.title(title)
				.body(body)
				.relatedEntityType(relatedEntityType)
				.relatedEntityId(relatedEntityId)
				.isRead(false)
				.build();
	}

	private Map<String, String> baseData(NotificationType type, RelatedEntityType relatedType,
			Long relatedId, User actor) {
		Map<String, String> data = new HashMap<>();
		data.put("type", type.name());
		data.put("relatedEntityType", relatedType.name());
		data.put("relatedEntityId", String.valueOf(relatedId));
		if (actor != null) {
			data.put("actorId", String.valueOf(actor.getId()));
			data.put("actorFirstName", actor.getFirstname() != null ? actor.getFirstname() : "");
		}
		return data;
	}

	private String preview(String content) {
		if (content == null) {
			return "";
		}
		if (content.length() <= BODY_PREVIEW_MAX) {
			return content;
		}
		return content.substring(0, BODY_PREVIEW_MAX - 3) + "...";
	}

	private DtoNotification toDto(Notification n) {
		DtoNotification dto = new DtoNotification();
		dto.setId(n.getId());
		dto.setType(n.getType());
		dto.setTitle(n.getTitle());
		dto.setBody(n.getBody());
		dto.setRelatedEntityType(n.getRelatedEntityType());
		dto.setRelatedEntityId(n.getRelatedEntityId());
		dto.setIsRead(n.getIsRead());
		dto.setCreatedAt(n.getCreatedAt());
		if (n.getActor() != null) {
			dto.setActorId(n.getActor().getId());
			dto.setActorFirstName(n.getActor().getFirstname());
			dto.setActorLastName(n.getActor().getLastname());
			dto.setActorAvatarUrl(n.getActor().getAvatarUrl());
		}
		return dto;
	}
}
