package com.ereniridere.service;

import org.springframework.data.domain.Page;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.dto.response.post.DtoPostSlice;
import com.ereniridere.dto.response.post.DtoToggleLike;
import com.ereniridere.entity.enums.PostType;

public interface IPostService {

	public DtoPost createPost(Integer userId, DtoCreatePost request);

	// Tek post detayı (bildirim üzerinden açıldığında veya derin link)
	public DtoPost getPostById(Integer userId, Integer postId);

	// ANA AKIŞ: Kendi postların da dahil mahalle duvarı (cursor/keyset tabanlı, PART 3).
	// type == SPONSORED && lat/lng verilmişse radius (metre) bazlı yakınlık filtresi,
	// aksi halde saf kronolojik keyset akış uygulanır.
	// cursor: bir önceki dilimin nextCursor'u (ilk sayfada null/boş).
	public DtoPostSlice getNeighborhoodFeed(Integer userId, PostType type, Double lat, Double lng, Integer radius,
			String cursor, int pageSize);

	public boolean deletePost(Integer userId, Integer postId);

	// PART 2: Mahalle akışında kullanıcının en son gördüğünden bu yana kaç yeni post var.
	// İlk çağrıda (lastSeenPostId null) taban çizgisini o anki en yeni post'a sabitler, 0 döner.
	public long getNewPostCount(Integer userId);

	// PART 2: "En son görülen" işaretini verilen post id'sine ilerletir (geri gitmez).
	public boolean markFeedSeen(Integer userId, Integer postId);

	// NORMAL PROFİLİM: Kendi bireysel postlarım (Standart ve Yardım)
	public Page<DtoPost> getMyPost(Integer userId, int pageNo, int pageSize);

	// ESNAF PROFİLİM: Kendi sponsorlu postlarım (YENİ EKLENEN İMZA)
	public Page<DtoPost> getMySponsoredPosts(Integer userId, int pageNo, int pageSize);

	public boolean updatePostText(Integer userId, Integer postId, DtoUpdatePost request);

	// String yerine DtoToggleLike dönüyoruz
	public DtoToggleLike toggleLike(Integer userId, Integer postId);

}