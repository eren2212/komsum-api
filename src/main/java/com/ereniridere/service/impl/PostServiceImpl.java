package com.ereniridere.service.impl;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
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
		eventPublisher.publishEvent(new PostCreatedEvent(savedPost));

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

	// 3. ANA AKIŞ (DİNAMİK FİLTRELİ)
	@Override
	public Page<DtoPost> getNeighborhoodFeed(Integer userId, PostType type, int pageNo, int pageSize) {
		User dbUser = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"Kanzi bir mahalleye kayıt olmadan duvarı göremezsin!"));
		}

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// DİKKAT: Artık 'type' parametresini de repository'e fırlatıyoruz
		Page<Post> postPage = postRepository.getNeighborhoodFeedExcludingMe(dbUser.getNeighborhood().getId(), userId,
				type, pageable);

		return postPage.map(post -> convertToDto(post, userId));
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
			dtoPost.setShopName(post.getAuthor().getMerchantProfile().getShopName());
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
