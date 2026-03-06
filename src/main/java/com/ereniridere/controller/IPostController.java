package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.entity.RootEntity;

public interface IPostController {

	public RootEntity<DtoPost> createPost(DtoCreatePost request);

	public RootEntity<Page<DtoPost>> getFeed(Integer pageNo, Integer pageSize);

	public RootEntity<Boolean> deletePost(Integer postId);

	public RootEntity<Page<DtoPost>> getMyPost(Integer pageNo, Integer pageSize);

	public RootEntity<Boolean> updatePostText(Integer postId, DtoUpdatePost request);

}
