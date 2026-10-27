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
		syncDocument(LegalDocumentType.AYDINLATMA_METNI, "KVKK Aydınlatma Metni", AYDINLATMA_METNI, 4);
		syncDocument(LegalDocumentType.KVKK, "Kişisel Verilerin Korunması ve İşlenmesi Sözleşmesi",
				KVKK_SOZLESMESI, 4);
		syncDocument(LegalDocumentType.KULLANIM_KOSULLARI, "Kullanım Şartları", KULLANIM_SARTLARI, 3);
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
	// Tanıtım sitesindeki (komsum-modern/src/legal/*.txt) metinlerle BİREBİR aynıdır;
	// mağazalara verilen adreslerle uygulamada onaylatılan metin aynı olmalı.
	// Metni değiştirirken önce .txt dosyasını güncelle, buraya aynen kopyala ve
	// yukarıdaki sürüm numarasını artır.

	private static final String AYDINLATMA_METNI = """
			SELAMKOMŞU KİŞİSEL VERİLERİN İŞLENMESİNE İLİŞKİN AYDINLATMA METNİ

			1. Veri Sorumlusu
			İşbu Aydınlatma Metni, 6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, veri sorumlusu sıfatıyla Eren İridere ("SelamKomşu" veya "Platform") tarafından, SelamKomşu mobil uygulaması kullanıcılarının kişisel verilerinin işlenmesine ilişkin usul ve esasların açıklanması amacıyla hazırlanmıştır.

			2. İşlenen Kişisel Veriler, İşleme Amaçları ve Hukuki Sebepler
			Platform, kişisel verilerinizi KVKK m. 5 ve 6'da belirtilen aşağıdaki hukuki sebeplere dayalı olarak işlemektedir:
			- Hesap ve Profil Bilgileri (Ad-Soyad, E-posta): Üyeliğin oluşturulması ve hesabın yönetilmesi amacıyla sözleşmenin kurulması ve ifası (m. 5/2-c) kapsamında işlenir.
			- Şifre Bilgisi: Veri güvenliğinin sağlanması amacıyla düz metin olarak tutulmaz; geri döndürülemez biçimde şifrelenmiş (hash'lenmiş) olarak saklanır.
			- Profil Fotoğrafı ve Biyografi: Tamamen isteğe bağlıdır. Kullanıcının kendi isteğiyle sunduğu bu veriler açık rıza (m. 5/1) kapsamında işlenir.
			- Beyan Edilen Konum (Mahalle/İlçe): Kayıtta seçtiğiniz mahalle, bölge bazlı içerik ve bildirim sunulabilmesi için sözleşmenin ifası ve meşru menfaat (m. 5/2-c, f) kapsamında işlenir.
			- Canlı Cihaz Konumu (GPS): Yalnızca konum izni vermeniz hâlinde ve uygulama açıkken kullanılır. Yakınınızdaki etkinlik ve esnafların listelenmesi için anlık sorgu olarak sunucuya iletilir; veritabanına kaydedilmez ve arka planda konum alınmaz. Bu izni dilediğiniz zaman cihaz ayarlarından kaldırabilirsiniz.
			- Paylaşılan İçerikler (Gönderi, Yorum, İlan, Etkinlik ve Fotoğraflar): Kullanıcının alenileştirme iradesi doğrultusunda mahalledeki diğer kullanıcılara gösterilmek üzere sözleşmenin ifası (m. 5/2-c) kapsamında işlenir.
			- Özel Mesajlar: Kullanıcılar arası mesajlaşma hizmetinin sunulması için sözleşmenin ifası (m. 5/2-c) kapsamında işlenir. Mesaj içerikleri sunucuda şifrelenmiş olarak saklanır.
			- Esnaf/İşletme Profil Verileri (Telefon, Adres, İşletme Konumu): Ticari faaliyetin yürütülmesi ve sözleşmenin ifası (m. 5/2-c) kapsamında işlenir.
			- Bildirim Kimliği: Bildirim izni vermeniz hâlinde, anlık bildirim gönderilebilmesi için cihazınızın bildirim kimliği işlenir.
			- Şikayet ve Engelleme Kayıtları: Uygunsuz içeriğin ve kötüye kullanımın önlenmesi amacıyla meşru menfaat (m. 5/2-f) kapsamında işlenir.

			3. Verilerin Saklandığı Yer ve Aktarılması
			Platform'un sunucu ve veritabanı altyapısı Türkiye'de bulunmaktadır. Kişisel verileriniz reklam amacıyla üçüncü kişilerle paylaşılmaz ve satılmaz.
			Hizmetin sunulabilmesi için yalnızca aşağıdaki aktarımlar yapılır:
			- Anlık Bildirimler: Bildirim kimliğiniz ile bildirimin başlık ve kısa içeriği, cihazınıza iletilmek üzere Expo (Expo Push Service), Apple (Apple Push Notification service) ve Google (Firebase Cloud Messaging) bildirim altyapıları üzerinden gönderilir.
			- E-posta: Şifre sıfırlama kodları, e-posta adresinize Google (Gmail) e-posta altyapısı üzerinden gönderilir.
			Bu hizmet sağlayıcıların sunucuları yurt dışında bulunduğundan, söz konusu aktarımlar KVKK m. 9 kapsamında yurt dışına aktarım niteliğindedir. Bildirim aktarımı, cihazınızda bildirim izni vermenizle gerçekleşir; izni dilediğiniz zaman cihaz ayarlarından kaldırabilirsiniz.

			4. Saklama Süresi
			Kişisel verileriniz hesabınız açık kaldığı sürece saklanır. Hesabınızı uygulama içinden (Profil > Hesabı Sil) sildiğinizde hesabınız ve hesabınıza bağlı tüm veriler kalıcı olarak silinir.

			5. İlgili Kişinin Hakları (KVKK m. 11)
			KVKK'nın 11. maddesi uyarınca; kişisel verilerinizin işlenip işlenmediğini öğrenme, işlenmişse buna ilişkin bilgi talep etme, işlenme amacını ve amacına uygun kullanılıp kullanılmadığını öğrenme, aktarıldığı üçüncü kişileri bilme, eksik veya yanlış işlenmişse düzeltilmesini ve silinmesini isteme haklarına sahipsiniz. Başvurularınızı appereniridere@gmail.com adresine iletebilirsiniz.
			""";

	private static final String KVKK_SOZLESMESI = """
			SELAMKOMŞU KİŞİSEL VERİLERİN KORUNMASI VE İŞLENMESİ SÖZLEŞMESİ

			1. Taraflar
			İşbu Kişisel Verilerin Korunması ve İşlenmesi Sözleşmesi ("Sözleşme"), bir tarafta veri sorumlusu sıfatını haiz Eren İridere (bundan böyle "SelamKomşu", "Platform" veya "Veri Sorumlusu" olarak anılacaktır) ile diğer tarafta platforma üye olan/kullanan Kullanıcı (bundan böyle "Kullanıcı" veya "İlgili Kişi" olarak anılacaktır) arasında, aşağıda belirtilen şartlar dahilinde akdedilmiştir.

			2. Sözleşmenin Konusu ve Amacı
			İşbu Sözleşme'nin konusu; Kullanıcı'nın Platform'u kullanımı sırasında elde edilen kişisel verilerinin, 6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK"), ilgili ikincil mevzuat ve emsal yargı/kurul kararları çerçevesinde işlenmesi, aktarılması, güvenliğinin sağlanması ve tarafların bu kapsamdaki hak ve yükümlülüklerinin belirlenmesidir.

			3. İşlenen Veri Kategorileri ve İşleme Amaçları
			Platform, Kullanıcı'ya ait verileri "belirli, açık ve meşru amaçlar için" ve "işlendikleri amaçla bağlantılı, sınırlı ve ölçülü olma" ilkelerine uygun olarak işlemektedir.

			3.1. Kullanıcı Hesap ve Profil Bilgileri
			- E-posta Adresi ve Ad-Soyad: Sözleşmenin ifası, hesap oluşturulması ve iletişim amacıyla işlenmektedir.
			- Şifre: Düz metin olarak tutulmamakta, kriptografik yöntemlerle (hash'lenmiş olarak) şifrelenerek saklanmaktadır.
			- Profil Fotoğrafı ve Biyografi: Kullanıcının tamamen kendi özgür iradesiyle (isteğe bağlı) sisteme yüklediği bu veriler, platform içi etkileşimi sağlamak amacıyla açık rızaya dayalı olarak işlenmektedir.

			3.2. Konum Verisi (Ayrıştırılmış İşleme Modeli)
			Konum verileri, ölçülülük ilkesi gereği iki bağımsız kategoriye ayrılmıştır:
			- Beyan Edilen Mahalle Bilgisi (Statik): Kullanıcının kayıt sırasında kendi beyanıyla seçtiği, GPS koordinatı içermeyen bölge bilgisidir. Veritabanında saklanır; Kullanıcı tarafından profilden değiştirilebilir.
			- Canlı Cihaz Konumu (GPS): Yalnızca Kullanıcı'nın konum izni vermesi hâlinde ve uygulama açıkken, Kullanıcı'ya yakın etkinlik ve esnafları listelemek amacıyla anlık sorgu olarak sunucuya iletilir. Bu veri veritabanında kalıcı veya geçici olarak saklanmaz ve arka planda konum alınmaz. Bu durum, veri minimizasyonu ve ölçülülük ilkesinin temel bir taahhüdüdür.

			3.3. Kullanıcılar Arası İletişim ve İçerik
			- Özel Mesajlaşma: Kullanıcılar arası mesaj içerikleri ve son mesaj özeti, "özel haberleşmenin gizliliği" esasına dayalı olarak şifrelenmiş biçimde saklanır. Platform, bu içerikler üzerinde keyfi bir denetim veya içerik analizi yapmaz.
			- Gönderi, Yorum, İlan ve Etkinlikler: Kullanıcının alenileştirme iradesi doğrultusunda diğer kullanıcılara açık şekilde yayınlanır. Diğer kullanıcılarca şikayet edilen içerikler, Kullanım Şartları'nın uygulanması amacıyla Platform tarafından incelenebilir.

			3.4. Esnaf/İşletme Profilleri
			İşletme sahibi kullanıcıların beyan ettiği telefon numarası, açık adres, işletme adı, kategorisi ve görseli ile işletmenin sabit konumuna ait koordinat bilgisi (enlem/boylam) ticari faaliyetin ifası amacıyla işlenir.
			Ayrıştırma Taahhüdü: İşletme koordinat bilgisi, ticari bir adres verisi olup, Kullanıcı'nın kişisel/canlı GPS verisinden tamamen bağımsızdır ve bu iki veri seti hiçbir şekilde birleştirilerek profilleme yapılamaz.

			4. Aydınlatma Yükümlülüğü ve Açık Rıza Mekanizması
			4.1. Süreçlerin Ayrıştırılması: Platform, Aydınlatma Metni'ni, işbu Sözleşme'yi ve Kullanım Şartları'nı ayrı belgeler olarak sunar; Kullanıcı her biri için ayrı kutucuk işaretler. Tek kutucukla tüm onayların alınması ("battaniye rıza") yöntemi kullanılmaz.
			4.2. Hizmet Şartına Bağlamama: Temel platform hizmetlerinin sunulması; profil fotoğrafı paylaşımı, canlı konum izni veya bildirim izni gibi isteğe bağlı veri işleme faaliyetlerine şart koşulamaz. Kullanıcı bu izinleri dilediği zaman, hizmet kaybına uğramadan cihaz ayarlarından geri çekebilir.

			5. Verilerin Saklandığı Yer ve Yurt Dışına Aktarım
			Platform'un sunucu ve veritabanı altyapısı Türkiye'de bulunmaktadır.
			Anlık bildirimlerin iletilmesi (Expo, Apple ve Google bildirim altyapıları) ve şifre sıfırlama e-postalarının gönderilmesi (Google Gmail altyapısı) için gerekli olan sınırlı veriler, sunucuları yurt dışında bulunan bu hizmet sağlayıcılar üzerinden iletilir. Bu aktarımlar KVKK m. 9 kapsamında yurt dışına aktarım niteliğindedir; ayrıntıları Aydınlatma Metni'nde açıklanmıştır.

			6. Veri Güvenliği, Doğruluk ve İspat Yükümlülüğü
			6.1. Doğruluk ve Kötüye Kullanımın Önlenmesi: Kullanıcı, sisteme girdiği iletişim bilgilerinin (e-posta vb.) doğru ve güncel olduğunu taahhüt eder. Platform, sahte hesapların ve kötüye kullanımın önlenmesi amacıyla şikayet ve engelleme mekanizmaları işletir ve ihlal tespit edilen hesapları askıya alabilir veya kapatabilir.
			6.2. Cihaz Güvenliği: Kullanıcı, kendi kişisel cihazının güvenliğini (antivirüs, şifre gizliliği) sağlamakla yükümlüdür. Kullanıcının cihazına bulaşan zararlı yazılımlar (trojan, phishing vb.) kaynaklı yetkisiz erişimlerden, Platform gerekli teknik tedbirleri almış olması şartıyla sorumlu tutulamaz.
			6.3. Teknik Tedbirler: Platform; şifrelerin hash'lenerek saklanması, özel mesajların şifrelenmesi, sunucuya erişimin yetkiyle sınırlandırılması ve iletişimin şifreli bağlantı (HTTPS) üzerinden yapılması gibi teknik ve idari tedbirleri almayı taahhüt eder.

			7. Rekabet Hukuku ve Veri Taşınabilirliği
			7.1. Veri Birleştirme Yasağı: Platform, farklı hizmet modüllerinden (örn. kişisel mesajlaşma ile işletme profilleri) elde ettiği verileri, kullanıcının açık ve ayrıştırılmış rızası olmaksızın birleştirerek rekabeti bozucu bir "kaldıraç etkisi" yaratamaz ve bu verileri üçüncü taraf iştirakleriyle haksız rekabet yaratacak şekilde paylaşamaz.
			7.2. Veri Taşınabilirliği: İşletme/Esnaf profili sahipleri, platforma kendi rızalarıyla yükledikleri adres ve ticari verilerini diledikleri zaman talep etme hakkına sahiptir. Talep hâlinde bu veriler makul bir sürede, yaygın kullanılan bir formatta ve bedelsiz olarak iletilir. Platform, bu hakkın kullanımını zorlaştırıcı münhasırlık şartları ileri süremez.

			8. Verilerin Saklanması ve İmhası
			Kişisel veriler, işlenme amaçlarının gerektirdiği süre boyunca ve hesap açık kaldığı sürece saklanır. Kullanıcı hesabını uygulama içinden sildiğinde hesabı ve hesabına bağlı tüm veriler kalıcı olarak silinir. Kanunlardan doğan saklama yükümlülükleri saklıdır.

			9. İlgili Kişinin Hakları (KVKK Madde 11)
			Kullanıcı, KVKK'nın 11. maddesi uyarınca; kişisel verilerinin işlenip işlenmediğini öğrenme, işlenmişse bilgi talep etme, işlenme amacına uygun kullanılıp kullanılmadığını öğrenme, yurt içinde/yurt dışında aktarıldığı üçüncü kişileri bilme, eksik/yanlış işlenmişse düzeltilmesini isteme ve verilerin silinmesini/yok edilmesini talep etme haklarına sahiptir. Başvurular appereniridere@gmail.com adresi üzerinden gerçekleştirilir.

			10. Yürürlük ve Kabul
			İşbu Sözleşme, Kullanıcı tarafından elektronik ortamda onaylandığı tarihte yürürlüğe girmiştir. Kullanıcı, Sözleşme'yi onaylamakla, Sözleşme'de yer alan tüm şartları okuduğunu, anladığını ve kabul ettiğini beyan eder. İşbu Sözleşme'den doğacak uyuşmazlıklarda, Platform'un dijital log kayıtları HMK m. 193 uyarınca kesin delil teşkil edecektir.
			""";

	private static final String KULLANIM_SARTLARI = """
			SELAMKOMŞU KULLANIM ŞARTLARI

			SelamKomşu'ya kayıt olarak aşağıdaki şartları kabul etmiş olursunuz. SelamKomşu, Eren İridere tarafından sunulmaktadır.

			1. Hizmet
			SelamKomşu, mahalle sakinlerinin gönderi, ilan ve etkinlik paylaşmasını, mahalledeki esnafa ulaşmasını ve birbiriyle mesajlaşmasını sağlayan bir komşuluk uygulamasıdır.

			2. Hesap
			- Uygulamayı kullanmak için 18 yaşından büyük olmalısınız.
			- Kayıtta doğru bilgi vermeli ve hesabınızın güvenliğinden siz sorumlu olmalısınız. Hesabınızı başkasına devredemezsiniz.

			3. Uygunsuz İçeriğe Sıfır Tolerans
			SelamKomşu'da uygunsuz içeriğe ve kötüye kullanıcılara kesinlikle tolerans gösterilmez. Aşağıdakileri paylaşmak ve yapmak yasaktır:
			- Hakaret, taciz, tehdit, zorbalık veya nefret söylemi,
			- Müstehcen, şiddet içeren veya rahatsız edici içerik,
			- Dolandırıcılık, yanıltıcı ilan, spam veya izinsiz reklam,
			- Başkalarının kişisel bilgilerini izinsiz paylaşmak,
			- Yasa dışı ürün, hizmet veya faaliyetler,
			- Başkasının yerine geçmek veya sahte hesap açmak.

			4. Şikayet ve Engelleme
			- Uygunsuz bulduğunuz gönderi, yorum ve ilanları uygulama içindeki menüden şikayet edebilirsiniz.
			- Rahatsız eden kullanıcıları engelleyebilirsiniz. Engellediğiniz kişiyle birbirinizin içeriklerini göremez ve mesajlaşamazsınız.
			- Şikayetler ve engellemeler en geç 24 saat içinde incelenir.

			5. Yaptırımlar
			Bu şartları ihlal eden içerikler önceden bildirim yapılmaksızın kaldırılabilir; ihlal eden kullanıcıların hesabı askıya alınabilir veya kalıcı olarak kapatılabilir. Birden fazla kullanıcı tarafından şikayet edilen içerikler, inceleme tamamlanana kadar otomatik olarak gizlenebilir.

			6. İçerik Sorumluluğu
			Paylaştığınız içerikten siz sorumlusunuz. İçeriğinizin uygulamada gösterilmesi için gerekli izni vermiş olursunuz; içeriğinizin sahipliği sizde kalır. Komşular arası alışveriş ve buluşmalar kullanıcıların kendi sorumluluğundadır; SelamKomşu bu işlemlerin tarafı değildir.

			7. Kişisel Veriler
			Kişisel verileriniz, KVKK Aydınlatma Metni ve Kişisel Verilerin Korunması ve İşlenmesi Sözleşmesi'nde açıklandığı şekilde işlenir.

			8. Hesabın Silinmesi
			Hesabınızı istediğiniz zaman uygulamadaki Profil > Hesabı Sil bölümünden kalıcı olarak silebilirsiniz.

			9. Değişiklikler
			Bu şartlar güncellenebilir. Önemli değişikliklerde uygulama üzerinden bilgilendirilirsiniz.

			10. İletişim
			Soru ve talepleriniz için appereniridere@gmail.com adresine yazabilir veya uygulamadaki Profil > Yardım ve Destek bölümünü kullanabilirsiniz.
			""";
}
