package com.ereniridere.controller;

import java.util.Map;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.notification.DtoSaveFcmToken;
import com.ereniridere.dto.response.notification.DtoNotification;
import com.ereniridere.entity.RootEntity;

public interface INotificationController {

	RootEntity<Page<DtoNotification>> getMyNotifications(int pageNo, int pageSize);

	RootEntity<Map<String, Long>> getUnreadCount();

	RootEntity<Void> markAsRead(Long id);

	RootEntity<Void> markAllAsRead();

	RootEntity<Void> saveFcmToken(DtoSaveFcmToken request);

	RootEntity<Void> deleteFcmToken();
}
