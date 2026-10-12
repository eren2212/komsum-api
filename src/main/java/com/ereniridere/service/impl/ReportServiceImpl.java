package com.ereniridere.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.request.report.DtoCreateReport;
import com.ereniridere.dto.response.report.DtoReport;
import com.ereniridere.entity.Comment;
import com.ereniridere.entity.MarketplaceListing;
import com.ereniridere.entity.Post;
import com.ereniridere.entity.Report;
import com.ereniridere.entity.User;
import com.ereniridere.entity.enums.ListingStatus;
import com.ereniridere.entity.enums.ReportStatus;
import com.ereniridere.entity.enums.ReportedEntityType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.CommentRepository;
import com.ereniridere.repository.MarketplaceListingRepository;
import com.ereniridere.repository.PostRepository;
import com.ereniridere.repository.ReportRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.IEmailService;
import com.ereniridere.service.IReportService;

@Service
public class ReportServiceImpl implements IReportService {

	private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

	// Bir içerik farklı kullanıcılar tarafından bu sayıya ulaşınca otomatik gizlenir.
	// Admin paneli yok; eşik aşıldığında log'a düşer, inceleme SQL üzerinden yapılır.
	private static final int AUTO_HIDE_THRESHOLD = 3;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private MarketplaceListingRepository marketplaceListingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private IEmailService emailService;

	@Override
	public DtoReport createReport(Integer userId, DtoCreateReport request) {

		Optional<User> optionalReporter = userRepository.findById(userId);
		if (optionalReporter.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı"));
		}

		ReportedEntityType type = request.getReportedEntityType();
		Integer entityId = request.getReportedEntityId();

		switch (type) {
			case POST -> validatePost(userId, entityId);
			case COMMENT -> validateComment(userId, entityId);
			case MARKETPLACE_LISTING -> validateMarketplaceListing(userId, entityId);
		}

		if (reportRepository.existsByReporterIdAndReportedEntityTypeAndReportedEntityId(userId, type, entityId)) {
			throw new BaseException(new ErrorMessage(MessageType.RECORD_ALREADY_EXISTS, "Bu içeriği zaten şikayet ettin"));
		}

		Report report = buildReport(optionalReporter.get(), request);

		Report savedReport = reportRepository.save(report);

		long reportCount = reportRepository.countByReportedEntityTypeAndReportedEntityId(type, entityId);

		User reporter = optionalReporter.get();
		emailService.sendModerationAlert("Yeni şikayet: " + type + " #" + entityId,
				"Şikayet eden: #" + reporter.getId() + " " + reporter.getFirstname() + " " + reporter.getLastname()
						+ "\nİçerik: " + type + " #" + entityId
						+ "\nSebep: " + request.getReason()
						+ "\nAçıklama: " + (request.getDescription() != null ? request.getDescription() : "-")
						+ "\nBu içerik için toplam şikayet: " + reportCount);

		if (reportCount >= AUTO_HIDE_THRESHOLD) {
			autoHide(type, entityId, reportCount);
		}

		return toDto(savedReport);
	}

	private Report buildReport(User reporter, DtoCreateReport request) {
		Report report = new Report();
		report.setReporter(reporter);
		report.setReportedEntityType(request.getReportedEntityType());
		report.setReportedEntityId(request.getReportedEntityId());
		report.setReason(request.getReason());
		report.setDescription(request.getDescription());
		report.setStatus(ReportStatus.PENDING);
		return report;
	}

	private void validatePost(Integer userId, Integer postId) {
		Optional<Post> optionalPost = postRepository.findById(postId);
		if (optionalPost.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Şikayet edilecek gönderi bulunamadı"));
		}
		Post post = optionalPost.get();
		if (!post.isActive()) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu gönderi zaten kaldırılmış"));
		}
		if (post.getAuthor().getId().equals(userId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kendi gönderini şikayet edemezsin"));
		}
	}

	private void validateComment(Integer userId, Integer commentId) {
		Optional<Comment> optionalComment = commentRepository.findById(commentId);
		if (optionalComment.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Şikayet edilecek yorum bulunamadı"));
		}
		Comment comment = optionalComment.get();
		if (!comment.isActive()) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu yorum zaten kaldırılmış"));
		}
		if (comment.getAuthor().getId().equals(userId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kendi yorumunu şikayet edemezsin"));
		}
	}

	private void validateMarketplaceListing(Integer userId, Integer listingId) {
		Optional<MarketplaceListing> optionalListing = marketplaceListingRepository.findById(listingId);
		if (optionalListing.isEmpty()) {
			throw new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Şikayet edilecek ilan bulunamadı"));
		}
		MarketplaceListing listing = optionalListing.get();
		if (listing.getStatus() != ListingStatus.ACTIVE) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Bu ilan zaten kaldırılmış"));
		}
		if (listing.getUser().getId().equals(userId)) {
			throw new BaseException(new ErrorMessage(MessageType.VALIDATION_FAILED, "Kendi ilanını şikayet edemezsin"));
		}
	}

	private void autoHide(ReportedEntityType type, Integer entityId, long reportCount) {
		switch (type) {
			case POST -> postRepository.findById(entityId).ifPresent(post -> {
				post.setActive(false);
				postRepository.save(post);
			});
			case COMMENT -> commentRepository.findById(entityId).ifPresent(comment -> {
				comment.setActive(false);
				commentRepository.save(comment);
			});
			case MARKETPLACE_LISTING -> marketplaceListingRepository.findById(entityId).ifPresent(listing -> {
				listing.setStatus(ListingStatus.DELETED);
				marketplaceListingRepository.save(listing);
			});
		}
		log.warn("İçerik otomatik gizlendi: type={} id={} reportCount={}", type, entityId, reportCount);
		emailService.sendModerationAlert("İçerik otomatik gizlendi: " + type + " #" + entityId,
				"İçerik " + reportCount + " farklı şikayet aldığı için otomatik gizlendi.\nİçerik: " + type + " #"
						+ entityId);
	}

	private DtoReport toDto(Report report) {
		DtoReport dto = new DtoReport();
		dto.setId(report.getId());
		dto.setReportedEntityType(report.getReportedEntityType());
		dto.setReportedEntityId(report.getReportedEntityId());
		dto.setReason(report.getReason());
		dto.setDescription(report.getDescription());
		dto.setStatus(report.getStatus() != null ? report.getStatus() : ReportStatus.PENDING);
		dto.setCreatedAt(report.getCreatedAt());
		return dto;
	}
}
