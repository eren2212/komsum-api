package com.ereniridere.service.impl;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.post.DtoCreatePost;
import com.ereniridere.dto.request.post.DtoUpdatePost;
import com.ereniridere.dto.response.post.DtoPost;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.PostLike;
import com.ereniridere.entity.User;
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

		Post newPost = new Post();

		// 1. İstekten gelen düz verileri (content, imageUrl, type) tek satırda Post'a
		// kopyala
		BeanUtils.copyProperties(request, newPost);

		// 2. Kancaları (İlişkileri) manuel olarak bağla
		newPost.setAuthor(dbUser);
		newPost.setNeighborhood(dbUser.getNeighborhood());

		// 3. Veritabanına kaydet
		Post savedPost = postRepository.save(newPost);

		// 4. Mobilde sadece göstereceğimiz verileri tutan DTO'yu hazırla
		DtoPost dtoPost = new DtoPost();

		// Post'taki düz verileri (id, content, type, imageUrl, createdAt) DTO'ya
		// kopyala
		BeanUtils.copyProperties(savedPost, dtoPost);

		// 5. Arayüzde sadece isim ve mahalle adı göstereceğimiz için,
		// gereksiz verileri almadan sadece bu spesifik alanları DTO'ya manuel
		// setliyoruz.

		dtoPost.setAuthorFirstName(dbUser.getFirstname());
		dtoPost.setAuthorLastName(dbUser.getLastname());
		dtoPost.setNeighborhoodName(dbUser.getNeighborhood().getName());

		return dtoPost;

	}

	@Override
	public Page<DtoPost> getNeighborhoodFeed(Integer userId, int pageNo, int pageSize) {
		Optional<User> optional = userRepository.findById(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		User dbUser = optional.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"Kanzi bir mahalleye kayıt olmadan duvarı göremezsin!"));
		}

		// 1. Sayfalama ayarlarını yap (Spring'de sayfalar 0'dan başlar, o yüzden
		// mobilden 1 gelirse 0'a çekiyoruz)
		Pageable pageable = PageRequest.of(pageNo, pageSize);

		// 2. Repository'den o efsane sorguyla postları çek!
		Page<Post> postPage = postRepository.getNeighborhoodFeed(dbUser.getNeighborhood().getId(), pageable);

		// 3. Gelen ağır Post objelerini, arayüzde kullanacağımız hafif DtoPost
		// objelerine çevir (.map metodu burada hayat kurtarır)
		return postPage.map(post -> {
			DtoPost dtoPost = new DtoPost();

			// Düz verileri kopyala
			BeanUtils.copyProperties(post, dtoPost);

			// Sadece ekranda lazım olan kancalı verileri ekle
			dtoPost.setAuthorFirstName(post.getAuthor().getFirstname());
			dtoPost.setAuthorLastName(post.getAuthor().getLastname());
			dtoPost.setNeighborhoodName(post.getNeighborhood().getName());

			return dtoPost;
		});

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

	@Override
	public Page<DtoPost> getMyPost(Integer userId, int pageNo, int pageSize) {
		Optional<User> optional = userRepository.findById(userId);

		if (optional.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		User dbUser = optional.get();

		if (dbUser.getNeighborhood() == null) {
			throw new BaseException(new ErrorMessage(MessageType.GENERAL_EXCEPTION,
					"Kanzi bir mahalleye kayıt olmadan duvarı göremezsin!"));
		}

		Pageable pageable = PageRequest.of(pageNo, pageSize);

		Page<Post> postPage = postRepository.findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable);
		return postPage.map(post -> {
			DtoPost dtoPost = new DtoPost();

			// Düz verileri kopyala
			BeanUtils.copyProperties(post, dtoPost);

			// Sadece ekranda lazım olan kancalı verileri ekle
			dtoPost.setAuthorFirstName(post.getAuthor().getFirstname());
			dtoPost.setAuthorLastName(post.getAuthor().getLastname());
			dtoPost.setNeighborhoodName(post.getNeighborhood().getName());

			return dtoPost;
		});
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

		// 2. Ekstra Güvenlik: Silinmiş bir postu güncelleyemez!
		if (!dbPost.isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Kanzi silinmiş bir gönderiyi güncelleyemezsin!"));
		}

		dbPost.setContent(request.getContent());

		postRepository.save(dbPost);

		return true;
	}

	@Override
	public String toggleLike(Integer userId, Integer postId) {

		Optional<Post> optionalPost = postRepository.findById(postId);

		if (optionalPost.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadı"));
		}

		if (!optionalPost.get().isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, " Silinmiş bir gönderiyi beğenemesin!"));
		}

		Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
		if (existingLike.isPresent()) {

			postLikeRepository.delete(existingLike.get());
			return "Dislike işlemi gerçekleştirildi";
		} else {
			Optional<User> optionalUser = userRepository.findById(userId);

			if (optionalUser.isEmpty()) {
				throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
			}
			PostLike newPostLike = new PostLike();

			newPostLike.setPost(optionalPost.get());
			newPostLike.setUser(optionalUser.get());
			postLikeRepository.save(newPostLike);

			return "Like işlemi gerçekleştirildi";

		}

	}

}
