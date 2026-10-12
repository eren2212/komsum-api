package com.ereniridere.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.Report;
import com.ereniridere.entity.enums.ReportedEntityType;

public interface ReportRepository extends JpaRepository<Report, Integer> {

	boolean existsByReporterIdAndReportedEntityTypeAndReportedEntityId(Integer reporterId,
			ReportedEntityType reportedEntityType, Integer reportedEntityId);

	long countByReportedEntityTypeAndReportedEntityId(ReportedEntityType reportedEntityType,
			Integer reportedEntityId);
}
