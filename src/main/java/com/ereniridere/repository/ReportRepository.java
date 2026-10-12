package com.ereniridere.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.Report;
import com.ereniridere.entity.enums.ReportedEntityType;

public interface ReportRepository extends JpaRepository<Report, Integer> {

	boolean existsByReporterIdAndReportedEntityTypeAndReportedEntityId(Integer reporterId,
			ReportedEntityType reportedEntityType, Integer reportedEntityId);

	long countByReportedEntityTypeAndReportedEntityId(ReportedEntityType reportedEntityType,
			Integer reportedEntityId);

	// Hesap silme: kullanıcının yaptığı şikayetler (reports.user_id FK'sı silmeyi engellemesin).
	@Modifying
	@Transactional
	@Query("DELETE FROM Report r WHERE r.reporter.id = :userId")
	void deleteAllByReporterId(@Param("userId") Integer userId);
}
