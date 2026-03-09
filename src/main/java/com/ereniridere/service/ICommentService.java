package com.ereniridere.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.post.DtoCreateComment;
import com.ereniridere.dto.response.post.DtoComment;

@Service
public interface ICommentService {

	public DtoComment createComment(Integer userId, Integer postId, DtoCreateComment request);

	public Page<DtoComment> getPostComments(Integer postId, int pageNo, int pageSize);

	public boolean deleteComment(Integer userId, Integer commentId);
}
