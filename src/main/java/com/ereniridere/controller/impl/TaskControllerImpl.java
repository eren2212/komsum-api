package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.ITaskController;
import com.ereniridere.dto.response.task.DtoDailyTask;
import com.ereniridere.dto.response.task.DtoPointsSummary;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.ITaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskControllerImpl extends BaseController implements ITaskController {

	@Autowired
	private ITaskService taskService;

	@GetMapping("/today")
	@Override
	public RootEntity<List<DtoDailyTask>> getTodayTasks() {
		return ok(taskService.getTodayTasks(currentUserId()));
	}

	@GetMapping("/me/summary")
	@Override
	public RootEntity<DtoPointsSummary> getMyPointsSummary() {
		return ok(taskService.getPointsSummary(currentUserId()));
	}

	private Integer currentUserId() {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return currentUser.getId();
	}
}
