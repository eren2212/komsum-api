package com.ereniridere.controller;

import java.util.List;

import com.ereniridere.dto.response.task.DtoDailyTask;
import com.ereniridere.dto.response.task.DtoPointsSummary;
import com.ereniridere.entity.RootEntity;

public interface ITaskController {

	RootEntity<List<DtoDailyTask>> getTodayTasks();

	RootEntity<DtoPointsSummary> getMyPointsSummary();
}
