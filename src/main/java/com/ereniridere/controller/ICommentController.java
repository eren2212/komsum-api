package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreateComment;
import com.ereniridere.dto.response.post.DtoComment;
import com.ereniridere.entity.RootEntity;

public interface ICommentController {

	public RootEntity<DtoComment> createComment(Integer postId, DtoCreateComment request);

	public RootEntity<Page<DtoComment>> getPostComments(Integer postId, int pageNo, int pageSize);

	public RootEntity<Boolean> deleteComment(Integer commentId);
}
