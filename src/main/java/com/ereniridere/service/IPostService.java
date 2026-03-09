package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;

public interface IPostService {
	public DtoPost createPost(Integer userId, DtoCreatePost request);

	public Page<DtoPost> getNeighborhoodFeed(Integer userId, int pageNo, int pageSize);

	public boolean deletePost(Integer userId, Integer postId);

	public Page<DtoPost> getMyPost(Integer userId, int pageNo, int pageSize);

	public boolean updatePostText(Integer userId, Integer postId, DtoUpdatePost request);

	public String toggleLike(Integer userId, Integer postId);

}
