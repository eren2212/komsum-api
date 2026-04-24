package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.dto.response.post.DtoToggleLike;

public interface IPostService {

	public DtoPost createPost(Integer userId, DtoCreatePost request);

	// ANA AKIŞ: Kendi postlarım hariç mahalle duvarı
	public Page<DtoPost> getNeighborhoodFeed(Integer userId, int pageNo, int pageSize);

	public boolean deletePost(Integer userId, Integer postId);

	// NORMAL PROFİLİM: Kendi bireysel postlarım (Standart ve Yardım)
	public Page<DtoPost> getMyPost(Integer userId, int pageNo, int pageSize);

	// ESNAF PROFİLİM: Kendi sponsorlu postlarım (YENİ EKLENEN İMZA)
	public Page<DtoPost> getMySponsoredPosts(Integer userId, int pageNo, int pageSize);

	public boolean updatePostText(Integer userId, Integer postId, DtoUpdatePost request);

	// String yerine DtoToggleLike dönüyoruz
	public DtoToggleLike toggleLike(Integer userId, Integer postId);

}