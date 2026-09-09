package com.ereniridere.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.dto.response.task.DtoBadge;
import com.ereniridere.dto.response.task.DtoDailyTask;
import com.ereniridere.dto.response.task.DtoPointsSummary;
import com.ereniridere.entity.Badge;
import com.ereniridere.entity.TaskCompletion;
import com.ereniridere.entity.TaskDefinition;
import com.ereniridere.entity.User;
import com.ereniridere.entity.UserBadge;
import com.ereniridere.entity.enums.TaskType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.BadgeRepository;
import com.ereniridere.repository.TaskCompletionRepository;
import com.ereniridere.repository.TaskDefinitionRepository;
import com.ereniridere.repository.UserBadgeRepository;
import com.ereniridere.repository.UserRepository;
import com.ereniridere.service.INotificationService;
import com.ereniridere.service.ITaskService;

@Service
public class TaskServiceImpl implements ITaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private TaskCompletionRepository taskCompletionRepository;

	@Autowired
	private BadgeRepository badgeRepository;

	@Autowired
	private UserBadgeRepository userBadgeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private INotificationService notificationService;

	@Override
	@Transactional
	public void awardPoints(Integer userId, TaskType type) {
		LocalDate today = LocalDate.now();

		if (taskCompletionRepository.existsByUserIdAndTaskTypeAndCompletionDate(userId, type, today)) {
			return;
		}

		Optional<TaskDefinition> definitionOpt = taskDefinitionRepository.findByTypeAndActiveTrue(type);
		if (definitionOpt.isEmpty()) {
			log.warn("Görev tanımı bulunamadı veya pasif, puan verilmedi (type={})", type);
			return;
		}
		TaskDefinition definition = definitionOpt.get();

		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return;
		}

		// Aynı anda gelen iki aksiyonda unique constraint ihlali fırlar; transaction
		// geri sarılır ve puan verilmez — istenen davranış budur. Hatayı çağıran
		// listener loglar.
		taskCompletionRepository.save(TaskCompletion.builder()
				.user(user)
				.taskType(type)
				.completionDate(today)
				.pointsAwarded(definition.getPointsValue())
				.build());

		int newTotal = (user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + definition.getPointsValue();
		user.setTotalPoints(newTotal);
		userRepository.save(user);

		grantEligibleBadges(user, newTotal);
	}

	@Override
	public List<DtoDailyTask> getTodayTasks(Integer userId) {
		Set<TaskType> completed = new HashSet<>(
				taskCompletionRepository.findCompletedTypes(userId, LocalDate.now()));

		List<DtoDailyTask> tasks = new ArrayList<>();
		for (TaskDefinition definition : taskDefinitionRepository.findByActiveTrue()) {
			tasks.add(DtoDailyTask.builder()
					.type(definition.getType())
					.title(definition.getTitle())
					.description(definition.getDescription())
					.pointsValue(definition.getPointsValue())
					.completed(completed.contains(definition.getType()))
					.build());
		}
		return tasks;
	}

	@Override
	public DtoPointsSummary getPointsSummary(Integer userId) {
		User user = userRepository.findById(userId).orElseThrow(
				() -> new BaseException(new ErrorMessage(MessageType.NO_RECORD_EXIST, "Kullanıcı bulunamadı")));

		int totalPoints = user.getTotalPoints() == null ? 0 : user.getTotalPoints();

		Map<Integer, UserBadge> earnedByBadgeId = new HashMap<>();
		for (UserBadge ub : userBadgeRepository.findByUserIdWithBadge(userId)) {
			earnedByBadgeId.put(ub.getBadge().getId(), ub);
		}

		List<DtoBadge> badges = new ArrayList<>();
		Badge nextBadge = null;
		for (Badge badge : badgeRepository.findAllByOrderByPointThresholdAsc()) {
			UserBadge earned = earnedByBadgeId.get(badge.getId());
			badges.add(DtoBadge.builder()
					.id(badge.getId())
					.name(badge.getName())
					.description(badge.getDescription())
					.pointThreshold(badge.getPointThreshold())
					.iconUrl(badge.getIconUrl())
					.earned(earned != null)
					.earnedAt(earned != null ? earned.getEarnedAt() : null)
					.build());

			if (earned == null && nextBadge == null) {
				nextBadge = badge;
			}
		}

		return DtoPointsSummary.builder()
				.totalPoints(totalPoints)
				.earnedBadgeCount(earnedByBadgeId.size())
				.badges(badges)
				.pointsToNextBadge(nextBadge == null ? null : Math.max(0, nextBadge.getPointThreshold() - totalPoints))
				.nextBadgeName(nextBadge == null ? null : nextBadge.getName())
				.build();
	}

	/**
	 * Yeni toplam puanla hak edilen ve henüz verilmemiş rozetleri verir. Puan
	 * eşiği birden fazla rozeti aynı anda açabileceği için hepsi dolaşılır.
	 */
	private void grantEligibleBadges(User user, int totalPoints) {
		Set<Integer> ownedBadgeIds = new HashSet<>(userBadgeRepository.findBadgeIdsByUserId(user.getId()));

		for (Badge badge : badgeRepository.findByPointThresholdLessThanEqual(totalPoints)) {
			if (ownedBadgeIds.contains(badge.getId())) {
				continue;
			}
			userBadgeRepository.save(UserBadge.builder()
					.user(user)
					.badge(badge)
					.build());
			notificationService.notifyBadgeEarned(user.getId(), badge.getId(), badge.getName());
		}
	}
}
