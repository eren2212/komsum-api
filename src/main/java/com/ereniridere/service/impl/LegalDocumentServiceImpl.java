package com.ereniridere.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ereniridere.dto.response.legal.DtoLegalDocument;
import com.ereniridere.entity.LegalDocument;
import com.ereniridere.entity.enums.LegalDocumentType;
import com.ereniridere.exception.BaseException;
import com.ereniridere.exception.ErrorMessage;
import com.ereniridere.exception.MessageType;
import com.ereniridere.repository.LegalDocumentRepository;
import com.ereniridere.service.ILegalDocumentService;

@Service
public class LegalDocumentServiceImpl implements ILegalDocumentService {

	@Autowired
	private LegalDocumentRepository legalDocumentRepository;

	@Override
	public List<DtoLegalDocument> getActiveDocuments() {
		return legalDocumentRepository.findByActiveTrue().stream().map(this::toDto).toList();
	}

	@Override
	public DtoLegalDocument getActiveDocumentByType(LegalDocumentType type) {
		LegalDocument doc = legalDocumentRepository.findByTypeAndActiveTrue(type)
				.orElseThrow(() -> new BaseException(
						new ErrorMessage(MessageType.NO_RECORD_EXIST, "İstenen yasal metin bulunamadı.")));
		return toDto(doc);
	}

	private DtoLegalDocument toDto(LegalDocument doc) {
		DtoLegalDocument dto = new DtoLegalDocument();
		dto.setId(doc.getId());
		dto.setType(doc.getType());
		dto.setTitle(doc.getTitle());
		dto.setContent(doc.getContent());
		dto.setVersion(doc.getVersion());
		return dto;
	}
}
