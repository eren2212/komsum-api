package com.ereniridere.dto.request.report;

import com.ereniridere.entity.enums.ReportReason;
import com.ereniridere.entity.enums.ReportedEntityType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoCreateReport {

	@NotNull(message = "Şikayet edilen içeriğin türü belirtilmeli !")
	private ReportedEntityType reportedEntityType;

	@NotNull(message = "Şikayet edilen içeriğin id'si belirtilmeli !")
	private Integer reportedEntityId;

	@NotNull(message = "Şikayet nedeni belirtilmeli !")
	private ReportReason reason;

	@Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir !")
	private String description;
}
