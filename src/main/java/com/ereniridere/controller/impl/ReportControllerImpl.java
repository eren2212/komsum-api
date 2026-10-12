package com.ereniridere.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.IReportController;
import com.ereniridere.dto.request.report.DtoCreateReport;
import com.ereniridere.dto.response.report.DtoReport;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.User;
import com.ereniridere.service.IReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ReportControllerImpl extends BaseController implements IReportController {

	@Autowired
	private IReportService reportService;

	@PostMapping("/reports")
	@Override
	public RootEntity<DtoReport> createReport(@Valid @RequestBody DtoCreateReport request) {

		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Integer userId = currentUser.getId();

		return ok(reportService.createReport(userId, request));
	}
}
