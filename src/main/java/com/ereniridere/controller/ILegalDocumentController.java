package com.ereniridere.controller;

import java.util.List;

import com.ereniridere.dto.response.legal.DtoLegalDocument;
import com.ereniridere.entity.RootEntity;
import com.ereniridere.entity.enums.LegalDocumentType;

public interface ILegalDocumentController {

	RootEntity<List<DtoLegalDocument>> getActiveDocuments();

	RootEntity<DtoLegalDocument> getByType(LegalDocumentType type);
}
