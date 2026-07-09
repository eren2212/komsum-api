package com.ereniridere.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ereniridere.controller.ILegalDocumentController;
import com.ereniridere.dto.response.legal.DtoLegalDocument;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.enums.LegalDocumentType;
import com.ereniridere.service.ILegalDocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalDocumentControllerImpl extends BaseController implements ILegalDocumentController {

	private final ILegalDocumentService legalDocumentService;

	/** GET /api/legal — yürürlükteki tüm yasal metinler */
	@GetMapping
	@Override
	public RootEntity<List<DtoLegalDocument>> getActiveDocuments() {
		return ok(legalDocumentService.getActiveDocuments());
	}

	/** GET /api/legal/{type} — KVKK veya AYDINLATMA_METNI */
	@GetMapping("/{type}")
	@Override
	public RootEntity<DtoLegalDocument> getByType(@PathVariable LegalDocumentType type) {
		return ok(legalDocumentService.getActiveDocumentByType(type));
	}
}
