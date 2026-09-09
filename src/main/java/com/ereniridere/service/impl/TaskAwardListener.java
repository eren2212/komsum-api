package com.ereniridere.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Post;
import com.ereniridere.entity.enums.TaskType;
import com.ereniridere.event.CommentCreatedEvent;
import com.ereniridere.event.EventParticipationEvent;
import com.ereniridere.event.PostCreatedEvent;
import com.ereniridere.repository.PostRepository;
import com.ereniridere.service.ITaskService;

/**
 * Komşu Görevi puanlarını domain event'lerden tetikler. Puanlama kullanıcı
 * akışını bloklamamalı ve hata vermesi asıl aksiyonu bozmamalı; bu yüzden async
 * çalışır ve tüm hatalar yutulup loglanır.
 */
@Component
public class TaskAwardListener {

	private static final Logger log = LoggerFactory.getLogger(TaskAwardListener.class);

	@Autowired
	private ITaskService taskService;

	@Autowired
	private PostRepository postRepository;

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handlePostCreated(PostCreatedEvent event) {
		try {
			Post post = postRepository.findById(event.getPostId()).orElse(null);
			if (post == null || post.getAuthor() == null) {
				return;
			}
			taskService.awardPoints(post.getAuthor().getId(), TaskType.CREATE_POST);
		} catch (Exception e) {
			log.error("Gönderi görevi puanı verilemedi (postId={})", event.getPostId(), e);
		}
	}

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handleEventParticipation(EventParticipationEvent event) {
		try {
			taskService.awardPoints(event.getUserId(), TaskType.JOIN_EVENT);
		} catch (Exception e) {
			log.error("Etkinlik görevi puanı verilemedi (userId={})", event.getUserId(), e);
		}
	}

	@Async("notificationExecutor")
	@EventListener
	@Transactional
	public void handleCommentCreated(CommentCreatedEvent event) {
		try {
			taskService.awardPoints(event.getAuthorId(), TaskType.WRITE_COMMENT);
		} catch (Exception e) {
			log.error("Yorum görevi puanı verilemedi (userId={})", event.getAuthorId(), e);
		}
	}
}
