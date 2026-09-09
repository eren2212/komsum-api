package com.ereniridere.service;

import java.util.List;

import com.ereniridere.dto.response.task.DtoDailyTask;
import com.ereniridere.dto.response.task.DtoPointsSummary;
import com.ereniridere.entity.enums.TaskType;

public interface ITaskService {

	/**
	 * Kullanıcıya ilgili görevin puanını verir. Aynı görev aynı gün ikinci kez
	 * tetiklenirse hiçbir şey yapmaz.
	 */
	void awardPoints(Integer userId, TaskType type);

	List<DtoDailyTask> getTodayTasks(Integer userId);

	DtoPointsSummary getPointsSummary(Integer userId);
}
