package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.comment.DtoCreateComment;
import com.ereniridere.dto.request.comment.DtoUpdateComment;
import com.ereniridere.dto.response.post.DtoComment;

public interface ICommentService {

	public DtoComment createComment(Integer userId, Integer postId, DtoCreateComment request);

	public Page<DtoComment> getPostComments(Integer postId, int pageNo, int pageSize);

	public boolean deleteComment(Integer userId, Integer commentId);

	public DtoComment updateComment(Integer userId, Integer commentId, DtoUpdateComment request);
}