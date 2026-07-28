package com.ereniridere.controller.impl;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IRoomioController;
import com.ereniridere.dto.request.roomio.DtoCreateRoomioProfile;
import com.ereniridere.dto.request.roomio.DtoSwipe;
import com.ereniridere.dto.request.roomio.DtoUpdateRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoRoomioProfile;
import com.ereniridere.dto.response.roomio.DtoSwipeResult;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.service.IRoomioService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roomio")
public class RoomioControllerImpl extends BaseController implements IRoomioController {

	@Autowired
	private IRoomioService roomioService;

	// Swipe işlemi için mevcut IRateLimitingService'ten (post beğenileri vb. ile
	// paylaşılan) BAĞIMSIZ, Roomio'ya özel kendi kovası — aynı kullanıcının başka
	// bir özellikteki hız sınırını burada tüketmesini engellemek için.
	private final Map<Integer, Bucket> swipeRateLimitCache = new ConcurrentHashMap<>();

	@PostMapping(path = "/profile")
	@Override
	public RootEntity<DtoRoomioProfile> createProfile(@Valid @RequestBody DtoCreateRoomioProfile request) {
		Integer userId = currentUserId();
		return ok(roomioService.createProfile(userId, request));
	}

	@PutMapping(path = "/profile")
	@Override
	public RootEntity<DtoRoomioProfile> updateProfile(@RequestBody DtoUpdateRoomioProfile request) {
		Integer userId = currentUserId();
		return ok(roomioService.updateProfile(userId, request));
	}

	@GetMapping(path = "/profile/me")
	@Override
	public RootEntity<DtoRoomioProfile> getMyProfile() {
		Integer userId = currentUserId();
		return ok(roomioService.getMyProfile(userId));
	}

	@PutMapping(path = "/profile/toggle-active")
	@Override
	public RootEntity<Boolean> toggleActive() {
		Integer userId = currentUserId();
		return ok(roomioService.toggleActive(userId));
	}

	@GetMapping(path = "/candidates")
	@Override
	public RootEntity<Page<DtoRoomioProfile>> getCandidateFeed(@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) Integer radius) {
		Integer userId = currentUserId();
		return ok(roomioService.getCandidateFeed(userId, pageNo, pageSize, radius));
	}

	@PostMapping(path = "/swipe")
	@Override
	public RootEntity<DtoSwipeResult> swipe(@Valid @RequestBody DtoSwipe request) {
		Integer userId = currentUserId();

		Bucket bucket = resolveSwipeBucket(userId);
		if (!bucket.tryConsume(1)) {
			throw new BaseException(new ErrorMessage(MessageType.TOO_MANY_REQUESTS, null));
		}

		return ok(roomioService.swipe(userId, request));
	}

	private Integer currentUserId() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return currentUser.getId();
	}

	private Bucket resolveSwipeBucket(Integer userId) {
		return swipeRateLimitCache.computeIfAbsent(userId, id -> {
			Bandwidth limit = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofSeconds(60)).build();
			return Bucket.builder().addLimit(limit).build();
		});
	}
}
