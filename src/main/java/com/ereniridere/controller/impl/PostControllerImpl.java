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

import com.ereniridere.controller.IPostController;
import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.dto.response.post.DtoPostSlice;
import com.ereniridere.dto.response.post.DtoToggleLike;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.PostType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.service.IPostService;
import com.ereniridere.service.IRateLimitingService;

import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
public class PostControllerImpl extends BaseController implements IPostController {

	@Autowired
	private IPostService postService;

	@Autowired
	private IRateLimitingService rateLimitingService;

	@PostMapping(path = "/create")
	@Override
	public RootEntity<DtoPost> createPost(@Valid @RequestBody DtoCreatePost request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		// 2. Adamın ID'sini al!
		Integer userId = currentUser.getId();

		return ok(postService.createPost(userId, request));
	}

	@GetMapping(path = "/{id}")
	@Override
	public RootEntity<DtoPost> getPostById(@PathVariable("id") Integer postId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return ok(postService.getPostById(currentUser.getId(), postId));
	}

	@PostMapping(path = "/delete/{id}")
	@Override
	public RootEntity<Boolean> deletePost(@PathVariable(value = "id") Integer postId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.deletePost(userId, postId));
	}

	// PART 2: Akışta okunmamış (en son görülenden sonraki) yeni post sayısı — rozet için.
	@GetMapping(path = "/feed/new-count")
	@Override
	public RootEntity<Long> getNewPostCount() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return ok(postService.getNewPostCount(currentUser.getId()));
	}

	// PART 2: "En son görülen" işaretini ilerlet (pull-to-refresh / "N yeni gönderi" tıklaması).
	@PostMapping(path = "/feed/mark-seen/{postId}")
	@Override
	public RootEntity<Boolean> markFeedSeen(@PathVariable("postId") Integer postId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return ok(postService.markFeedSeen(currentUser.getId(), postId));
	}

	// ANA AKIŞ: Kendi postların da dahil (PART 3). Cursor (keyset) tabanlı — offset yok.
	// cursor: bir önceki dilimin nextCursor'u; ilk sayfada gönderilmez (null).
	// type=SPONSORED + lat/lng verilirse radius (metre) bazlı yakınlık filtresi uygulanır;
	// aksi halde saf kronolojik keyset akış (lat/lng/radius opsiyonel).
	@GetMapping(path = "/feed")
	@Override
	public RootEntity<DtoPostSlice> getFeed(@RequestParam(required = false) PostType type,
			@RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
			@RequestParam(defaultValue = "5000") Integer radius, @RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "10") Integer pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(postService.getNeighborhoodFeed(userId, type, lat, lng, radius, cursor, pageSize));
	}

	// NORMAL PROFİLİM: Kendi bireysel postlarım
	@GetMapping(path = "/my-posts")
	@Override
	public RootEntity<Page<DtoPost>> getMyPost(@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();
		return ok(postService.getMyPost(userId, pageNo, pageSize));
	}

	// ESNAF PROFİLİM: Kendi sponsorlu postlarım (YENİ EKLENDİ - IPostController'a
	// imzasını at!)
	@GetMapping(path = "/my-sponsored-posts")
	public RootEntity<Page<DtoPost>> getMySponsoredPosts(@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();
		return ok(postService.getMySponsoredPosts(userId, pageNo, pageSize));
	}

	@PutMapping("/update/{id}")
	@Override
	public RootEntity<Boolean> updatePostText(@PathVariable(value = "id") Integer postId,
			@Valid @RequestBody DtoUpdatePost request) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.updatePostText(userId, postId, request));
	}

	@PostMapping("/{id}/like")
	@Override
	public RootEntity<DtoToggleLike> toogleLike(@PathVariable(value = "id") Integer postId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		Bucket bucket = rateLimitingService.resolveBucket(userId);

		if (bucket.tryConsume(1)) {
			// Artık ok(...) içine DTO yolluyoruz!
			return ok(postService.toggleLike(userId, postId));
		} else {
			throw new BaseException(new ErrorMessage(MessageType.TOO_MANY_REQUESTS, null));
		}
	}

}
