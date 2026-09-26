package com.ereniridere.service.impl;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.comment.DtoCreateComment;
import com.ereniridere.dto.request.comment.DtoUpdateComment;
import com.ereniridere.dto.response.post.DtoComment;
import com.ereniridere.entity.Comment;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.User;
import com.ereniridere.event.CommentCreatedEvent;
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

	@Autowired
	private ApplicationEventPublisher eventPublisher;

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

		if (request.getParentCommentId() != null) {
			Comment parent = commentRepository.findById(request.getParentCommentId())
					.orElseThrow(() -> new BaseException(
							new ErrorMessage(MessageType.NO_RECORD_EXIST, "Cevap vermek istediğin yorum bulunamadı")));

			if (!parent.getPost().getId().equals(postId)) {
				throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu yorum bu gönderiye ait değil"));
			}

			// Tek seviye threading: bir cevaba cevap verilirse, orijinal üst yoruma bağlanır
			Comment effectiveParent = parent.getParentComment() != null ? parent.getParentComment() : parent;

			if (!effectiveParent.isActive()) {
				throw new BaseException(
						new ErrorMessage(MessageType.VALIDATION_FAILED, "Silinmiş bir yoruma cevap veremezsin!"));
			}

			newComment.setParentComment(effectiveParent);
		}

		Comment saveComment = commentRepository.save(newComment);

		eventPublisher.publishEvent(new CommentCreatedEvent(saveComment.getId(), userId));

		DtoComment dtoComment = new DtoComment();

		BeanUtils.copyProperties(saveComment, dtoComment);
		dtoComment.setAuthorId(optionalUser.get().getId());
		dtoComment.setAuthorFirstName(optionalUser.get().getFirstname());
		dtoComment.setAuthorLastName(optionalUser.get().getLastname());
		dtoComment.setParentCommentId(saveComment.getParentComment() != null ? saveComment.getParentComment().getId() : null);

		return dtoComment;
	}

	@Override
	public Page<DtoComment> getPostComments(Integer postId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Comment> comments = commentRepository
				.findByPostIdAndParentCommentIsNullAndIsActiveTrueOrderByCreatedAtAsc(postId, pageable);

		return comments.map(this::toDto);
	}

	@Override
	public Page<DtoComment> getCommentReplies(Integer commentId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<Comment> replies = commentRepository.findByParentCommentIdAndIsActiveTrueOrderByCreatedAtAsc(commentId,
				pageable);

		return replies.map(this::toDto);
	}

	private DtoComment toDto(Comment comment) {
		DtoComment dto = new DtoComment();
		BeanUtils.copyProperties(comment, dto);
		dto.setAuthorId(comment.getAuthor().getId());
		dto.setAuthorFirstName(comment.getAuthor().getFirstname());
		dto.setAuthorLastName(comment.getAuthor().getLastname());
		dto.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
		return dto;
	}


	@Override
	public DtoComment updateComment(Integer userId, Integer commentId, DtoUpdateComment request) {

		Optional<Comment> optionalComment = commentRepository.findById(commentId);

		if (optionalComment.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Yorum bulunamadı"));
		}

		Comment dbComment = optionalComment.get();

		// 1. Güvenlik: Silinmiş bir yorumu güncelleyemez!
		if (!dbComment.isActive()) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Silinmiş bir yorumu güncelleyemezsin kanzi!"));
		}

		// 2. Güvenlik: Başkasının yorumunu güncelleyemez! (En kritik nokta)
		if (!dbComment.getAuthor().getId().equals(userId)) {
			throw new BaseException(
					new ErrorMessage(MessageType.VALIDATION_FAILED, "Başkasının yorumunu düzenleyemezsin!"));
		}

		// Kontrolleri geçtik, yorumu güncelle
		dbComment.setContent(request.getContent());
		Comment updatedComment = commentRepository.save(dbComment);

		// Mobilde hemen gösterebilmek için DTO'ya çevirip dön
		return toDto(updatedComment);
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
