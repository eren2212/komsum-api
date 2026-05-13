package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.response.notification.DtoNotification;

public interface INotificationService {

	Page<DtoNotification> getMyNotifications(Integer userId, int pageNo, int pageSize);

	long getUnreadCount(Integer userId);

	void markAsRead(Integer userId, Long notificationId);

	void markAllAsRead(Integer userId);

	void saveFcmToken(Integer userId, String token);

	void clearFcmToken(Integer userId);
}
