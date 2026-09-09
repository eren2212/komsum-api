package com.ereniridere.dto.response.task;

import com.ereniridere.entity.enums.TaskType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoDailyTask {

	private TaskType type;
	private String title;
	private String description;
	private Integer pointsValue;
	private boolean completed;
}
