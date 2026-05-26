package com.ereniridere.controller;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.dto.response.post.DtoToggleLike;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.enums.PostType;

public interface IPostController {

	public RootEntity<DtoPost> createPost(DtoCreatePost request);

	public RootEntity<DtoPost> getPostById(Integer postId);

	public RootEntity<Page<DtoPost>> getFeed(PostType type, Double lat, Double lng, Integer radius, Integer pageNo,
			Integer pageSize);

	public RootEntity<Boolean> deletePost(Integer postId);

	public RootEntity<Page<DtoPost>> getMyPost(Integer pageNo, Integer pageSize);

	public RootEntity<Page<DtoPost>> getMySponsoredPosts(Integer pageNo, Integer pageSize);

	public RootEntity<Boolean> updatePostText(Integer postId, DtoUpdatePost request);

	// String yerine DtoToggleLike dönüyoruz
	public RootEntity<DtoToggleLike> toogleLike(Integer postId);

}