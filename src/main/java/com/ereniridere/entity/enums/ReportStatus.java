package com.ereniridere.entity.enums;

// Şikayetler şu an için bir admin panelinden değil, doğrudan veritabanından
// (SQL ile) incelenir. Bu alan manuel inceleme sırasında SQL UPDATE ile güncellenir.
public enum ReportStatus {
	PENDING, RESOLVED
}
