package com.ereniridere.service;

import com.ereniridere.dto.request.report.DtoCreateReport;
import com.ereniridere.dto.response.report.DtoReport;

public interface IReportService {

	public DtoReport createReport(Integer userId, DtoCreateReport request);
}
