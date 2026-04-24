package com.ereniridere.controller.impl;

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
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.ICommentController;
import com.ereniridere.dto.request.comment.DtoCreateComment;
import com.ereniridere.dto.request.comment.DtoUpdateComment;
import com.ereniridere.dto.response.post.DtoComment;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.ICommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")

public class CommentControllerImpl extends BaseController implements ICommentController {

	@Autowired
	private ICommentService commentService;

	@PostMapping("/posts/{postId}/comments")
	@Override
	public RootEntity<DtoComment> createComment(@PathVariable(value = "postId") Integer postId,
			@Valid @RequestBody DtoCreateComment request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(commentService.createComment(userId, postId, request));
	}

	@GetMapping("/posts/{postId}/comments")
	@Override
	public RootEntity<Page<DtoComment>> getPostComments(@PathVariable(value = "postId") Integer postId, int pageNo,
			int pageSize) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(commentService.getPostComments(postId, pageNo, pageSize));
	}

	@PutMapping("/comments/{commentId}")
	@Override
	public RootEntity<DtoComment> updateComment(@PathVariable(value = "commentId") Integer commentId,
			@Valid @RequestBody DtoUpdateComment request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(commentService.updateComment(userId, commentId, request));
	}

	@DeleteMapping("/comments/{commentId}")
	@Override
	public RootEntity<Boolean> deleteComment(@PathVariable(value = "commentId") Integer commentId) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(commentService.deleteComment(userId, commentId));
	}

}
