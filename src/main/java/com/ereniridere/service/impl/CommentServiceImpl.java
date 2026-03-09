package com.ereniridere.service.impl;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.post.DtoCreateComment;
import com.ereniridere.dto.response.post.DtoComment;
import com.ereniridere.entity.Comment;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.User;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.CommentRepository;
import com.ereniridere.repository.PostRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.ICommentService;

@Service
public class CommentServiceImpl implements ICommentService {

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public DtoComment createComment(Integer userId, Integer postId, DtoCreateComment request) {

		Optional<User> optionalUser = userRepository.findById(userId);

		if (optionalUser.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		Optional<Post> optionalPost = postRepository.findById(postId);

		if (optionalPost.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Post bulunamadı"));
		}

		if (!optionalPost.get().isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Silinmiş bir gönderiye yorum yapamazsın!"));
		}

		Comment newComment = new Comment();

		newComment.setAuthor(optionalUser.get());
		newComment.setContent(request.getContent());
		newComment.setPost(optionalPost.get());

		Comment saveComment = commentRepository.save(newComment);

		DtoComment dtoComment = new DtoComment();

		BeanUtils.copyProperties(saveComment, dtoComment);
		dtoComment.setAuthorFirstName(optionalUser.get().getFirstname());
		dtoComment.setAuthorLastName(optionalUser.get().getLastname());

		return dtoComment;
	}

	@Override
	public Page<DtoComment> getPostComments(Integer postId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Comment> comments = commentRepository.findByPostIdAndIsActiveTrueOrderByCreatedAtAsc(postId, pageable);

		return comments.map(comment -> {
			DtoComment dto = new DtoComment();
			BeanUtils.copyProperties(comment, dto);
			dto.setAuthorFirstName(comment.getAuthor().getFirstname());
			dto.setAuthorLastName(comment.getAuthor().getLastname());
			return dto;
		});
	}

	@Override
	public boolean deleteComment(Integer userId, Integer commentId) {

		Optional<Comment> optionalComment = commentRepository.findById(commentId);

		if (optionalComment.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Yorum bulunamadı"));
		}
		Comment dbComment = optionalComment.get();

		// Güvenlik: Başkasının yorumunu silemez!
		if (!dbComment.getAuthor().getId().equals(userId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Başkasının yorumunu silemezsin!"));
		}

		dbComment.setActive(false);
		commentRepository.save(dbComment);
		return true;
	}

}
