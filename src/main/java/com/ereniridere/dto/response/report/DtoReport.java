package com.ereniridere.dto.response.report;

import java.time.LocalDateTime;

import com.ereniridere.entity.enums.ReportReason;
import com.ereniridere.entity.enums.ReportStatus;
import com.ereniridere.entity.enums.ReportedEntityType;

import lombok.Data;

@Data
public class DtoReport {

	private Integer id;

	private ReportedEntityType reportedEntityType;

	private Integer reportedEntityId;

	private ReportReason reason;

	private String description;

	private ReportStatus status;

	private LocalDateTime createdAt;
}
