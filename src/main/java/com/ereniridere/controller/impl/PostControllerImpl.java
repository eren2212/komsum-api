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
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IPostService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
public class PostControllerImpl extends BaseController implements IPostController {

	@Autowired
	private IPostService postService;

	@PostMapping(path = "/create")
	@Override
	public RootEntity<DtoPost> createPost(@Valid @RequestBody DtoCreatePost request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		// 2. Adamın ID'sini al!
		Integer userId = currentUser.getId();

		return ok(postService.createPost(userId, request));
	}

	@GetMapping(path = "/feed")
	@Override
	public RootEntity<Page<DtoPost>> getFeed(@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {

		// Kendi ID'mizi kasadan çekiyoruz
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.getNeighborhoodFeed(userId, pageNo, pageSize));
	}

	@PostMapping(path = "/delete/{id}")
	@Override
	public RootEntity<Boolean> deletePost(@PathVariable(value = "id") Integer postId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.deletePost(userId, postId));
	}

	@GetMapping(path = "/my-posts")
	@Override
	public RootEntity<Page<DtoPost>> getMyPost(@RequestParam(defaultValue = "0") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.getMyPost(userId, pageNo, pageSize));
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
	public RootEntity<String> toogleLike(@PathVariable(value = "id") Integer postId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Integer userId = currentUser.getId();

		return ok(postService.toggleLike(userId, postId));
	}

}
