package com.ereniridere.entity.enums;

/**
 * "Gerekli Kişiler / Ustalar" rehberindeki meslek kategorileri.
 * Sabit liste — mobilde filtreleme (çip) ve tutarlı görünüm için enum tutulur.
 * Yeni meslek gerekirse buraya eklenir (mobil label map'i de güncellenmeli).
 */
public enum ServiceCategory {
	OGRETMEN, // Öğretmen
	OZEL_DERS, // Özel ders
	SU_TESISATCISI, // Su tesisatçısı
	ELEKTRIKCI, // Elektrikçi
	BOYACI, // Boyacı
	MARANGOZ, // Marangoz
	TADILAT, // Tadilat / inşaat
	TEMIZLIK, // Temizlik
	BAKICI, // Çocuk / yaşlı bakıcısı
	NAKLIYAT, // Nakliyat / taşımacılık
	KLIMA_BEYAZ_ESYA, // Klima & beyaz eşya servisi
	BAHCIVAN, // Bahçıvan
	KUAFOR_GUZELLIK, // Kuaför & güzellik
	TERZI, // Terzi
	DIGER // Diğer
}
