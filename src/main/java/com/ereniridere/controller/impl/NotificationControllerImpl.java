package com.ereniridere.controller.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.INotificationController;
import com.ereniridere.dto.request.notification.DtoSaveFcmToken;
import com.ereniridere.dto.response.notification.DtoNotification;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.INotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationControllerImpl extends BaseController implements INotificationController {

	@Autowired
	private INotificationService notificationService;

	@GetMapping
	@Override
	public RootEntity<Page<DtoNotification>> getMyNotifications(
			@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "20") int pageSize) {
		Integer userId = currentUserId();
		return ok(notificationService.getMyNotifications(userId, pageNo, pageSize));
	}

	@GetMapping("/unread-count")
	@Override
	public RootEntity<Map<String, Long>> getUnreadCount() {
		Integer userId = currentUserId();
		long count = notificationService.getUnreadCount(userId);
		return ok(Map.of("count", count));
	}

	@PutMapping("/{id}/read")
	@Override
	public RootEntity<Void> markAsRead(@PathVariable("id") Long id) {
		Integer userId = currentUserId();
		notificationService.markAsRead(userId, id);
		return ok(null);
	}

	@PutMapping("/read-all")
	@Override
	public RootEntity<Void> markAllAsRead() {
		Integer userId = currentUserId();
		notificationService.markAllAsRead(userId);
		return ok(null);
	}

	@PostMapping("/fcm-token")
	@Override
	public RootEntity<Void> saveFcmToken(@Valid @RequestBody DtoSaveFcmToken request) {
		Integer userId = currentUserId();
		notificationService.saveFcmToken(userId, request.getToken());
		return ok(null);
	}

	@DeleteMapping("/fcm-token")
	@Override
	public RootEntity<Void> deleteFcmToken() {
		Integer userId = currentUserId();
		notificationService.clearFcmToken(userId);
		return ok(null);
	}

	private Integer currentUserId() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return currentUser.getId();
	}
}
