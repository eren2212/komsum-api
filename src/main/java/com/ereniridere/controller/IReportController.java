package com.ereniridere.controller;

import com.ereniridere.dto.request.report.DtoCreateReport;
import com.ereniridere.dto.response.report.DtoReport;
import com.ereniridere.entity.RootEntity;

public interface IReportController {

	public RootEntity<DtoReport> createReport(DtoCreateReport request);
}
