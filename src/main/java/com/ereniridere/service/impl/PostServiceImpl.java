package com.ereniridere.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.dto.response.post.DtoPostSlice;
import com.ereniridere.dto.response.post.DtoToggleLike;
import com.ereniridere.entity.MerchantProfile;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.PostLike;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.PostType;
import com.ereniridere.event.PostCreatedEvent;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.PostLikeRepository;
import com.ereniridere.repository.PostRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.security.filter.JwtAuthenticationFilter;
import com.ereniridere.service.IPostService;
import com.ereniridere.util.GeoUtils;

@Service
public class PostServiceImpl implements IPostService {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostLikeRepository postLikeRepository;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	PostServiceImpl(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Override
	public DtoPost getPostById(Integer userId, Integer postId) {
		Post post = postRepository.findById(postId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadı")));
		if (!post.isActive()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Bu gönderi silinmiş"));
		}
		return convertToDto(post, userId);
	}

	@Override
	public DtoPost createPost(Integer userId, DtoCreatePost request) {

		Optional<User> optional = userRepository.findById(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		User dbUser = optional.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"Kanzi bir mahalleye kayıt olmadan gönderi paylaşamazsın!"));
		}

		if (request.getType() == PostType.SPONSORED) {
			// Eğer adam SPONSORED atmaya çalışıyorsa, dükkanı var mı ve onaylı mı
			// bakıyoruz!
			MerchantProfile esnaf = dbUser.getMerchantProfile();

			if (esnaf == null) {
				throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
						"Esnaf profilin yok, sponsorlu post atamazsın!"));
			}
			if (!esnaf.isVerified()) {
				throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED,
						"Esnaf profilin henüz onaylanmamış. Onaylanana kadar dükkan adına post atamazsın!"));
			}
		}
		Post newPost = new Post();

		// 1. İstekten gelen düz verileri (content, imageUrl, type) tek satırda Post'a
		// kopyala
		BeanUtils.copyProperties(request, newPost);

		// 2. Kancaları (İlişkileri) manuel olarak bağla
		newPost.setAuthor(dbUser);
		newPost.setNeighborhood(dbUser.getNeighborhood());

		// 3. Veritabanına kaydet
		Post savedPost = postRepository.save(newPost);

		// 3.1 Async bildirim akışını tetikle (ilçedeki diğer kullanıcılara FCM + inbox)
		eventPublisher.publishEvent(new PostCreatedEvent(savedPost.getId()));

		// 4. Mobilde sadece göstereceğimiz verileri tutan DTO'yu hazırla
		DtoPost dtoPost = new DtoPost();

		// Post'taki düz verileri (id, content, type, imageUrl, createdAt) DTO'ya
		// kopyala
		BeanUtils.copyProperties(savedPost, dtoPost);

		// 5. Arayüzde sadece isim ve mahalle adı göstereceğimiz için,
		// gereksiz verileri almadan sadece bu spesifik alanları DTO'ya manuel
		// setliyoruz.

		dtoPost.setAuthorId(dbUser.getId());
		dtoPost.setAuthorFirstName(dbUser.getFirstname());
		dtoPost.setAuthorLastName(dbUser.getLastname());
		dtoPost.setNeighborhoodName(dbUser.getNeighborhood().getName());
		if (dbUser.getMerchantProfile() != null) {
			dtoPost.setShopName(dbUser.getMerchantProfile().getShopName());
		}

		return dtoPost;

	}

	@Override
	public boolean updatePostText(Integer userId, Integer postId, DtoUpdatePost request) {

		Optional<Post> optional = postRepository.findById(postId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadı"));
		}

		Post dbPost = optional.get();

		if (!dbPost.getAuthor().getId().equals(userId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Kullanıcının böyle bir postu yok"));
		}

		// Ekstra Güvenlik: Silinmiş bir postu güncelleyemez!
		if (!dbPost.isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Kanzi silinmiş bir gönderiyi güncelleyemezsin!"));
		}

		dbPost.setContent(request.getContent());

		postRepository.save(dbPost);

		return true;
	}

	@Override
	public boolean deletePost(Integer userId, Integer postId) {

		Optional<User> optional = userRepository.findById(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadi"));
		}

		Optional<Post> optional2 = postRepository.findById(postId);

		if (optional2.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadi"));
		}

		Post post = optional2.get();

		if (!post.getAuthor().getId().equals(userId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu kullanıcya ait böyle bir post yok"));
		}
		post.setActive(false);
		postRepository.save(post);
		return true;
	}

	// 3. ANA AKIŞ (DİNAMİK FİLTRELİ) — cursor (keyset) tabanlı
	@Override
	public DtoPostSlice getNeighborhoodFeed(Integer userId, PostType type, Double lat, Double lng, Integer radius,
			String cursor, int pageSize) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"Kanzi bir mahalleye kayıt olmadan duvarı göremezsin!"));
		}

		// 🚨 SPONSORED + konum varsa: yakınlık (radius) bazlı esnaf akışı (iki-adımlı).
		// Bu akış mesafe sıralı olduğundan keyset uygulanamaz; mevcut offset davranışını
		// koruyup opak sayfa-numarası cursor'u ile aynı slice sözleşmesine sarmalıyoruz.
		if (type == PostType.SPONSORED && lat != null && lng != null) {
			int pageNo = parsePageCursor(cursor);
			Page<DtoPost> page = getSponsoredNearbyFeed(userId, lat, lng, radius, PageRequest.of(pageNo, pageSize));
			return DtoPostSlice.builder().content(page.getContent())
					.nextCursor(page.hasNext() ? String.valueOf(pageNo + 1) : null).hasNext(page.hasNext()).build();
		}

		// Standart kronolojik akış → keyset. Cursor "createdAt|id" formatında (ilk sayfada null).
		LocalDateTime cursorTime = null;
		Integer cursorId = null;
		if (cursor != null && !cursor.isBlank()) {
			int sep = cursor.lastIndexOf('|');
			if (sep > 0) {
				cursorTime = LocalDateTime.parse(cursor.substring(0, sep));
				cursorId = Integer.parseInt(cursor.substring(sep + 1));
			}
		}

		// hasNext'i tespit etmek için 1 fazla çek; fazlaysa son elemanı at.
		// cursorTime null ise (ilk sayfa) ayrı bir sorguya gidiyoruz — bkz. PostRepository'deki not
		// (PostgreSQL, TIMESTAMP parametresinin ":x IS NULL" dalında tipini çıkaramıyor).
		List<Post> rows = cursorTime == null
				? postRepository.getNeighborhoodFeedFirstPage(dbUser.getNeighborhood().getId(), type,
						PageRequest.of(0, pageSize + 1))
				: postRepository.getNeighborhoodFeedAfterCursor(dbUser.getNeighborhood().getId(), type, cursorTime,
						cursorId, PageRequest.of(0, pageSize + 1));

		boolean hasNext = rows.size() > pageSize;
		if (hasNext) {
			rows = rows.subList(0, pageSize);
		}

		List<DtoPost> dtos = rows.stream().map(post -> convertToDto(post, userId)).collect(Collectors.toList());

		String nextCursor = null;
		if (hasNext && !rows.isEmpty()) {
			Post lastRow = rows.get(rows.size() - 1);
			nextCursor = lastRow.getCreatedAt().toString() + "|" + lastRow.getId();
		}

		return DtoPostSlice.builder().content(dtos).nextCursor(nextCursor).hasNext(hasNext).build();
	}

	// PART 2: "En son görülenden bu yana kaç yeni post" (mahalle scope'u, kendi postlarım hariç)
	@Override
	public long getNewPostCount(Integer userId) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			return 0;
		}
		Integer neighborhoodId = dbUser.getNeighborhood().getId();

		// İlk kez: taban çizgisi henüz yok → şu anki en yeni post'a sabitle ve 0 dön
		// (kullanıcı akışı ilk açtığında koca bir "N yeni" rozeti görmesin).
		if (dbUser.getLastSeenPostId() == null) {
			Integer maxId = postRepository.findMaxPostIdInScope(neighborhoodId, userId);
			if (maxId != null) {
				userRepository.advanceLastSeenPostId(userId, maxId);
			}
			return 0;
		}

		return postRepository.countNewPostsSince(neighborhoodId, userId, dbUser.getLastSeenPostId());
	}

	// PART 2: "En son görülen" işaretini ilerlet (geri gitmez).
	@Override
	public boolean markFeedSeen(Integer userId, Integer postId) {
		if (postId == null || postId <= 0) {
			return false;
		}
		userRepository.advanceLastSeenPostId(userId, postId);
		return true;
	}

	// Sponsorlu yakınlık akışının opak cursor'u yalnızca sayfa numarası taşır.
	private int parsePageCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return 0;
		}
		try {
			int page = Integer.parseInt(cursor.trim());
			return page < 0 ? 0 : page;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// 🚨 SPONSORED YAKINLIK AKIŞI: native ID sorgusu + JOIN FETCH hidrasyonu (N+1 yok)
	private Page<DtoPost> getSponsoredNearbyFeed(Integer userId, Double lat, Double lng, Integer radius,
			Pageable pageable) {

		int radiusMeters = (radius == null || radius <= 0) ? 5000 : radius;

		// ADIM 1: Spatial index ile radius içindeki sponsorlu post ID sayfası (en yakın önce)
		Page<Integer> idPage = postRepository.findSponsoredNearbyPostIds(userId, lat, lng, radiusMeters, pageable);

		if (idPage.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
		}

		List<Integer> ids = idPage.getContent();

		// ADIM 2: ID'lerden JOIN FETCH ile hidrate et
		Map<Integer, Post> byId = postRepository.findAllByIdInWithFetch(ids).stream()
				.collect(Collectors.toMap(Post::getId, Function.identity()));

		// Native sorgunun mesafe sıralamasını koruyarak DTO listesi üret
		List<DtoPost> dtos = ids.stream().map(byId::get).filter(Objects::nonNull)
				.map(post -> convertToDto(post, userId)).collect(Collectors.toList());

		return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
	}

	// 4. KENDİ BİREYSEL POSTLARIM
	@Override
	public Page<DtoPost> getMyPost(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Post> postPage = postRepository.getMyStandardPosts(userId, pageable);
		return postPage.map(post -> convertToDto(post, userId));
	}

	// 5. KENDİ ESNAF POSTLARIM
	@Override
	public Page<DtoPost> getMySponsoredPosts(Integer userId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Post> postPage = postRepository.getMySponsoredPosts(userId, pageable);
		return postPage.map(post -> convertToDto(post, userId));
	}

	// 🚨 SENIOR DOKUNUŞU: DTO Dönüşüm metodu güncellendi 🚨
	private DtoPost convertToDto(Post post, Integer currentUserId) {
		DtoPost dtoPost = new DtoPost();
		BeanUtils.copyProperties(post, dtoPost);

		dtoPost.setType(post.getType());
		dtoPost.setAuthorId(post.getAuthor().getId());
		dtoPost.setAuthorKarmaScore(post.getAuthor().getKarmaScore());
		dtoPost.setAuthorFirstName(post.getAuthor().getFirstname());
		dtoPost.setAuthorLastName(post.getAuthor().getLastname());
		dtoPost.setAuthorAvatarUrl(post.getAuthor().getAvatarUrl());
		dtoPost.setNeighborhoodName(post.getNeighborhood().getName());

		// YENİ EKLENEN SAYAÇLAR VE KONTROLLER
		dtoPost.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
		dtoPost.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);

		// Bu postu okuyan adam (currentUserId) daha önce beğenmiş mi?
		boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
		dtoPost.setLikedByMe(isLiked);

		if (post.getType() == PostType.SPONSORED && post.getAuthor().getMerchantProfile() != null) {
			MerchantProfile esnaf = post.getAuthor().getMerchantProfile();
			dtoPost.setShopName(esnaf.getShopName());
			// SPONSORED postta esnafın dükkan konumunu (harita pini) DTO'ya geçir
			dtoPost.setLatitude(GeoUtils.getLatitude(esnaf.getGeoLocation()));
			dtoPost.setLongitude(GeoUtils.getLongitude(esnaf.getGeoLocation()));
		}
		return dtoPost;
	}

	// 🚨 INSTAGRAM GİBİ LİKE SİSTEMİ 🚨
	@Override
	public DtoToggleLike toggleLike(Integer userId, Integer postId) {

		Optional<Post> optionalPost = postRepository.findById(postId);
		if (optionalPost.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadı"));
		}

		if (!optionalPost.get().isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Silinmiş bir gönderiyi beğenemesin!"));
		}

		Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
		boolean isLiked;

		if (existingLike.isPresent()) {
			postLikeRepository.delete(existingLike.get());
			isLiked = false; // Beğeniyi çektik
		} else {
			User user = userRepository.findById(userId).orElseThrow(
					() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

			PostLike newPostLike = new PostLike();
			newPostLike.setPost(optionalPost.get());
			newPostLike.setUser(user);
			postLikeRepository.save(newPostLike);
			isLiked = true; // Yeni beğendik
		}

		// Güncel beğeni sayısını veritabanından çek (PostLikeRepository'de
		// countByPostId yoksa yazmalısın)
		Integer newLikeCount = postLikeRepository.countByPostId(postId);

		// Ekranda kalp kırmızı mı olsun ve sayı kaç yazsın? Al sana cevap:
		return new DtoToggleLike(isLiked, newLikeCount);
	}

}
