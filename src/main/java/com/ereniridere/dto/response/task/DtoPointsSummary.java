package com.ereniridere.dto.response.task;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoPointsSummary {

	private Integer totalPoints;
	private Integer earnedBadgeCount;
	// Tüm rozetler; kazanılmayanlar da ilerleme göstermek için döner.
	private List<DtoBadge> badges;
	// Bir sonraki rozete kalan puan; tüm rozetler kazanıldıysa null.
	private Integer pointsToNextBadge;
	private String nextBadgeName;
}
