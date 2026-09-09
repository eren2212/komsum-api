package com.ereniridere.dto.response.task;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoBadge {

	private Integer id;
	private String name;
	private String description;
	private Integer pointThreshold;
	private String iconUrl;
	private boolean earned;
	// Sadece kazanılmış rozetlerde dolu.
	private LocalDateTime earnedAt;
}
