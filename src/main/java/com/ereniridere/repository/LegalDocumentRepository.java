package com.ereniridere.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ereniridere.entity.LegalDocument;
import com.ereniridere.entity.enums.LegalDocumentType;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Integer> {

	List<LegalDocument> findByActiveTrue();

	Optional<LegalDocument> findByTypeAndActiveTrue(LegalDocumentType type);

	boolean existsByType(LegalDocumentType type);
}
