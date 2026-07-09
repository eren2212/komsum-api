package com.ereniridere.service;

import java.util.List;

import com.ereniridere.dto.response.legal.DtoLegalDocument;
import com.ereniridere.entity.enums.LegalDocumentType;

public interface ILegalDocumentService {

	List<DtoLegalDocument> getActiveDocuments();

	DtoLegalDocument getActiveDocumentByType(LegalDocumentType type);
}
