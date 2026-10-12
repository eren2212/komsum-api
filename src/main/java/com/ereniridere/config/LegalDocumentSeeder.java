package com.ereniridere.config;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ereniridere.entity.LegalDocument;
import com.ereniridere.entity.enums.LegalDocumentType;
import com.ereniridere.repository.LegalDocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Uygulama açılışında yasal metinleri (Aydınlatma, Açık Rıza, Kullanım Koşulları)
 * koddaki sürümle eşitler. Her metin için:
 * <ul>
 * <li>Hiç yoksa → verilen sürümle eklenir.</li>
 * <li>Yürürlükteki sürüm koddakinden eskiyse → eskisi pasife alınır, yeni sürüm
 * YENİ SATIR olarak eklenir. Eski satır silinmez: user_consents kayıtları hangi
 * kullanıcının hangi sürümü onayladığını göstermeye devam eder.</li>
 * <li>Aynı ya da daha yeni sürüm yürürlükteyse → dokunulmaz (DB'den elle
 * güncellenmiş metinler ezilmez).</li>
 * </ul>
 * Metni değiştirmek için içeriği güncelleyip sürüm numarasını artırmak yeterli.
 */
@Component
@RequiredArgsConstructor
public class LegalDocumentSeeder implements CommandLineRunner {

	private final LegalDocumentRepository legalDocumentRepository;

	@Override
	@Transactional
	public void run(String... args) {
		syncDocument(LegalDocumentType.AYDINLATMA_METNI, "KVKK Aydınlatma Metni", AYDINLATMA_METNI, 2);
		syncDocument(LegalDocumentType.KVKK, "Açık Rıza Metni", ACIK_RIZA_METNI, 2);
		syncDocument(LegalDocumentType.KULLANIM_KOSULLARI, "Kullanım Koşulları", KULLANIM_KOSULLARI, 1);
	}

	private void syncDocument(LegalDocumentType type, String title, String content, int version) {
		Optional<LegalDocument> current = legalDocumentRepository.findByTypeAndActiveTrue(type);

		if (current.isPresent() && current.get().getVersion() >= version) {
			return;
		}

		current.ifPresent(doc -> {
			doc.setActive(false);
			legalDocumentRepository.save(doc);
		});

		legalDocumentRepository.save(LegalDocument.builder()
				.type(type)
				.title(title)
				.content(content)
				.version(version)
				.active(true)
				.build());
	}

	// ─── Metinler ────────────────────────────────────────────────────────────────

	private static final String AYDINLATMA_METNI = """
			SELAMKOMŞU UYGULAMASI KİŞİSEL VERİLERİN İŞLENMESİNE İLİŞKİN AYDINLATMA METNİ

			1. Veri Sorumlusu
			6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, kişisel
			verileriniz veri sorumlusu sıfatıyla SelamKomşu tarafından aşağıda
			açıklanan kapsamda işlenmektedir.

			2. İşlenen Kişisel Veriler
			- Kimlik ve iletişim: ad, soyad, e-posta adresi.
			- Hesap güvenliği: şifre (geri döndürülemez biçimde şifrelenmiş olarak).
			- Konum: kayıtta seçtiğiniz mahalle; izin vermeniz hâlinde yakınınızdaki
			  esnaf ve etkinlikleri göstermek için uygulama açıkken anlık konumunuz
			  (arka planda konum alınmaz).
			- İçerik: paylaştığınız gönderi, yorum, ilan, etkinlik ve fotoğraflar.
			- İletişim: komşularınızla yaptığınız mesajlaşmaların içeriği (sunucuda
			  şifrelenmiş olarak saklanır).
			- Cihaz: anlık bildirim gönderebilmek için cihazınızın bildirim kimliği.
			- Güvenlik ve moderasyon: yaptığınız şikayetler ve engellemeler.

			3. Kişisel Verilerin İşlenme Amaçları
			- Üyelik kaydının oluşturulması ve hesabınızın yönetilmesi,
			- Mahalle bazlı gönderi, etkinlik, ilan ve esnafların gösterilmesi,
			- Komşular arası mesajlaşma hizmetinin sağlanması,
			- Bildirimlerin iletilmesi,
			- Uygunsuz içerik ve kötüye kullanımın önlenmesi, şikayetlerin incelenmesi,
			- Güvenliğin sağlanması ve yasal yükümlülüklerin yerine getirilmesi.

			4. Kişisel Verilerin Aktarılması
			Verileriniz; hizmetin sağlanması için kullanılan sunucu ve altyapı hizmet
			sağlayıcılarına ve anlık bildirimlerin iletilmesi için bildirim hizmet
			sağlayıcılarına (Expo, Apple, Google), yalnızca gerekli olduğu ölçüde ve
			yasal sınırlar dahilinde aktarılabilir. Verileriniz reklam amacıyla
			üçüncü kişilerle paylaşılmaz ve satılmaz.

			5. Toplama Yöntemi ve Hukuki Sebep
			Verileriniz, uygulamaya kayıt ve kullanım sırasında elektronik ortamda; bir
			sözleşmenin kurulması ve ifası, veri sorumlusunun meşru menfaati ve açık
			rızanıza dayanarak toplanır.

			6. Saklama Süresi
			Verileriniz hesabınız açık kaldığı sürece saklanır. Hesabınızı uygulama
			içinden (Profil > Hesabı Sil) sildiğinizde hesabınız ve ona bağlı tüm
			veriler kalıcı olarak silinir.

			7. KVKK Kapsamındaki Haklarınız
			KVKK'nın 11. maddesi uyarınca; kişisel verilerinizin işlenip işlenmediğini
			öğrenme, işlenmişse buna ilişkin bilgi talep etme, düzeltilmesini veya
			silinmesini isteme ve diğer haklarınızı kullanma hakkına sahipsiniz.
			Taleplerinizi uygulamadaki Profil > Yardım ve Destek bölümünde yer alan
			iletişim adresi üzerinden iletebilirsiniz.
			""";

	private static final String ACIK_RIZA_METNI = """
			SELAMKOMŞU UYGULAMASI AÇIK RIZA METNİ

			6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında hazırlanan
			Aydınlatma Metni'ni okuduğumu ve anladığımı beyan ederim.

			SelamKomşu uygulamasını kullanabilmem için; ad, soyad, e-posta,
			mahalle/konum bilgilerimin, uygulama içi paylaşımlarım ile mesajlaşma
			içeriklerimin, Aydınlatma Metni'nde belirtilen amaçlarla işlenmesine ve
			hizmetin sağlanması için gerekli altyapı ve bildirim hizmet sağlayıcılarına
			aktarılmasına açık rızam ile onay veriyorum.

			Bu rızayı dilediğim zaman geri alabileceğimi ve hesabımı uygulama içinden
			silebileceğimi bildiğimi kabul ederim.
			""";

	private static final String KULLANIM_KOSULLARI = """
			SELAMKOMŞU KULLANIM KOŞULLARI

			SelamKomşu'ya kayıt olarak aşağıdaki koşulları kabul etmiş olursunuz.

			1. Hizmet
			SelamKomşu, mahalle sakinlerinin gönderi, ilan, etkinlik paylaşmasını ve
			birbiriyle mesajlaşmasını sağlayan bir komşuluk uygulamasıdır.

			2. Hesap
			- Uygulamayı kullanmak için 18 yaşından büyük olmalısınız.
			- Kayıtta doğru bilgi vermeli, hesabınızın güvenliğinden siz sorumlu
			  olmalısınız. Hesabınızı başkasına devredemezsiniz.

			3. Uygunsuz İçeriğe Sıfır Tolerans
			SelamKomşu'da uygunsuz içeriğe ve kötüye kullanıcılara kesinlikle
			tolerans gösterilmez. Aşağıdakileri paylaşmak ve yapmak yasaktır:
			- Hakaret, taciz, tehdit, zorbalık veya nefret söylemi,
			- Müstehcen, şiddet içeren veya rahatsız edici içerik,
			- Dolandırıcılık, yanıltıcı ilan, spam veya izinsiz reklam,
			- Başkalarının kişisel bilgilerini izinsiz paylaşmak,
			- Yasa dışı ürün, hizmet veya faaliyetler,
			- Başkasının yerine geçmek veya sahte hesap açmak.

			4. Şikayet ve Engelleme
			- Uygunsuz bulduğunuz gönderi, yorum ve ilanları uygulama içinden
			  şikayet edebilirsiniz.
			- Rahatsız eden kullanıcıları engelleyebilirsiniz. Engellediğiniz kişiyle
			  birbirinizin içeriklerini göremez ve mesajlaşamazsınız.
			- Şikayetler ve engellemeler en geç 24 saat içinde incelenir.

			5. Yaptırımlar
			Bu koşulları ihlal eden içerikler önceden bildirim yapılmaksızın
			kaldırılabilir; ihlal eden kullanıcıların hesabı askıya alınabilir veya
			kalıcı olarak kapatılabilir. Birden fazla kullanıcı tarafından şikayet
			edilen içerikler inceleme tamamlanana kadar otomatik olarak gizlenebilir.

			6. İçerik Sorumluluğu
			Paylaştığınız içerikten siz sorumlusunuz. İçeriğinizi uygulamada
			gösterilmesi amacıyla kullanmamıza izin vermiş olursunuz; içeriğinizin
			sahipliği sizde kalır. Komşular arası alışveriş ve buluşmalar kullanıcıların
			kendi sorumluluğundadır; SelamKomşu bu işlemlerin tarafı değildir.

			7. Hesabın Silinmesi
			Hesabınızı istediğiniz zaman Profil > Hesabı Sil bölümünden kalıcı
			olarak silebilirsiniz.

			8. Değişiklikler
			Bu koşullar güncellenebilir. Önemli değişikliklerde uygulama üzerinden
			bilgilendirilirsiniz.

			9. İletişim
			Soru ve talepleriniz için uygulamadaki Profil > Yardım ve Destek
			bölümünde yer alan iletişim adresini kullanabilirsiniz.
			""";
}
