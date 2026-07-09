package com.ereniridere.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ereniridere.entity.LegalDocument;
import com.ereniridere.entity.enums.LegalDocumentType;
import com.ereniridere.repository.LegalDocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Uygulama açılışında KVKK ve Aydınlatma metni tablosu boşsa varsayılan Türkçe
 * taslak metinleri ekler. Type bazında idempotenttir — metin zaten varsa
 * dokunmaz. İçerikler daha sonra DB üzerinden güncellenebilir.
 */
@Component
@RequiredArgsConstructor
public class LegalDocumentSeeder implements CommandLineRunner {

	private final LegalDocumentRepository legalDocumentRepository;

	@Override
	public void run(String... args) {
		seedIfAbsent(LegalDocumentType.AYDINLATMA_METNI,
				"KVKK Aydınlatma Metni",
				AYDINLATMA_METNI);
		seedIfAbsent(LegalDocumentType.KVKK,
				"Açık Rıza Metni",
				ACIK_RIZA_METNI);
	}

	private void seedIfAbsent(LegalDocumentType type, String title, String content) {
		if (!legalDocumentRepository.existsByType(type)) {
			legalDocumentRepository.save(LegalDocument.builder()
					.type(type)
					.title(title)
					.content(content)
					.version(1)
					.active(true)
					.build());
		}
	}

	// ─── Taslak metinler (sonradan DB'den güncellenebilir) ───────────────────────

	private static final String AYDINLATMA_METNI = """
			KOMŞUM UYGULAMASI KİŞİSEL VERİLERİN İŞLENMESİNE İLİŞKİN AYDINLATMA METNİ

			1. Veri Sorumlusu
			6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, kişisel
			verileriniz veri sorumlusu sıfatıyla Komşum tarafından aşağıda açıklanan
			kapsamda işlenmektedir.

			2. İşlenen Kişisel Veriler
			Ad, soyad, e-posta adresi, şifre (şifrelenmiş olarak), mahalle/konum bilgisi,
			uygulama içi paylaşımlarınız ve mesajlaşma içerikleriniz işlenmektedir.

			3. Kişisel Verilerin İşlenme Amaçları
			- Üyelik kaydının oluşturulması ve hesabınızın yönetilmesi,
			- Mahalle bazlı içerik, etkinlik ve ilanların gösterilmesi,
			- Komşular arası mesajlaşma hizmetinin sağlanması,
			- Güvenliğin sağlanması ve yasal yükümlülüklerin yerine getirilmesi.

			4. Kişisel Verilerin Aktarılması
			Verileriniz; hizmetin sağlanması için kullanılan altyapı ve bulut hizmet
			sağlayıcılarına, yalnızca gerekli olduğu ölçüde ve yasal sınırlar dahilinde
			aktarılabilir.

			5. Toplama Yöntemi ve Hukuki Sebep
			Verileriniz, uygulamaya kayıt ve kullanım sırasında elektronik ortamda; bir
			sözleşmenin kurulması ve ifası ile meşru menfaat hukuki sebeplerine dayanarak
			toplanır.

			6. KVKK Kapsamındaki Haklarınız
			KVKK'nın 11. maddesi uyarınca; kişisel verilerinizin işlenip işlenmediğini
			öğrenme, düzeltilmesini veya silinmesini isteme ve diğer haklarınızı
			kullanma hakkına sahipsiniz.

			Bu metin örnek/taslak niteliğindedir; yayın öncesi hukuki onaydan
			geçirilmelidir.
			""";

	private static final String ACIK_RIZA_METNI = """
			KOMŞUM UYGULAMASI AÇIK RIZA METNİ

			6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında hazırlanan
			Aydınlatma Metni'ni okuduğumu ve anladığımı beyan ederim.

			Komşum uygulamasını kullanabilmem için; ad, soyad, e-posta, mahalle/konum
			bilgilerimin ve uygulama içi paylaşımlarım ile mesajlaşma içeriklerimin,
			Aydınlatma Metni'nde belirtilen amaçlarla işlenmesine ve hizmetin
			sağlanması için gerekli altyapı/bulut hizmet sağlayıcılarına aktarılmasına;

			açık rızam ile onay veriyorum.

			Bu rızayı dilediğim zaman geri alabileceğimi bildiğimi kabul ederim.

			Bu metin örnek/taslak niteliğindedir; yayın öncesi hukuki onaydan
			geçirilmelidir.
			""";
}
