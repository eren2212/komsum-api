package com.ereniridere.dto.response.legal;

import com.ereniridere.entity.enums.LegalDocumentType;

import lombok.Data;

@Data
public class DtoLegalDocument {
	private Integer id;
	private LegalDocumentType type;
	private String title;
	private String content;
	private Integer version;
}
