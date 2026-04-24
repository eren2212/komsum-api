package com.ereniridere.dto.response.post;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DtoToggleLike {

	private boolean isLiked; // İşlem sonrası beğendim mi?

	private Integer newLikeCount; // İşlem sonrası toplam sayı kaç oldu?
}