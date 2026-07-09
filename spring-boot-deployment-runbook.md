# Spring Boot + PostgreSQL Dağıtım El Kitabı (Deployment Runbook)

> **Senaryo:** Elimizde yazılmış bir Spring Boot projesi var ve şu an Supabase (bulut PostgreSQL + Supabase Storage) kullanıyoruz. Amacımız Supabase'i tamamen bırakıp her şeyi (veritabanı + dosya deposu) kendi Ubuntu VPS'imize (4 GB RAM) taşımak. Veriler henüz boş/az ve uygulama canlı değil (dev/test aşaması), bu yüzden taşıma işlemi düşük riskli.
>
> Profesyonel bir şirkette nasıl yapılırsa öyle, Docker ile uygulamayı, PostgreSQL'i ve dosya deposu olarak MinIO'yu (Supabase Storage'ın self-hosted, S3 uyumlu karşılığı) ayağa kaldıracağız. Ayrıca Supabase'deki tablo yapısını ve dosyaları kendi sunucumuza taşıyacağız (Bölüm 12-13).
>
> Bu doküman bir *runbook*'tur: gerçek şirketlerde bir sistem nasıl kurulur/güncellenir, adım adım yazılı tutulur. Sunucunun başında bunu açık tutup sırayla uygulayın.

---

## 0. Mimari — Ne kuruyoruz, neden böyle?

Kuracağımız yapı şöyle görünecek:

```
                    İnternet
                       │
                       ▼
              ┌──────────────────┐
              │   Nginx (80/443) │  ← (İleri adım) Domain + SSL
              └────────┬─────────┘
                       │ (sunucu içi)
            ┌──────────▼───────────┐
            │  Spring Boot (app)   │  Docker container, port 8080
            └─────┬───────────┬────┘
                  │           │   "backend" özel ağ (sadece sunucu içi)
       ┌──────────▼──────┐  ┌─▼──────────────────┐
       │ PostgreSQL (db) │  │ MinIO (storage)    │  S3 uyumlu dosya deposu
       │ + kalıcı veri   │  │ + kalıcı veri      │  (Supabase Storage yerine)
       └─────────────────┘  └────────────────────┘
```

> **Not (Supabase'den geçiş):** Şu an veritabanı Supabase'de, dosyalar Supabase Storage'da. Bu kurulumda ikisini de yukarıdaki kendi container'larımıza taşıyoruz. PostgreSQL Supabase Postgres'in yerini, MinIO ise Supabase Storage'ın yerini alır. Taşıma adımları Bölüm 12 ve 13'te.

**Temel prensipler (neden böyle yapıyoruz):**

- **Her servis kendi "kutusunda" (container):** Uygulama ve veritabanı birbirine karışmaz. Birini silip yeniden kurmak diğerini etkilemez. Gerçek şirketlerde standart budur.
- **Veritabanı dışarıya kapalı:** PostgreSQL portunu (5432) internete *açmayacağız*. Sadece uygulama, sunucu içindeki özel ağ üzerinden veritabanına ulaşır. Bu, en sık yapılan güvenlik hatasını (veritabanını internete açmak) baştan engeller.
- **Veri kalıcı (volume):** Container silinse bile veritabanı verisi kaybolmaz; ayrı bir "volume" alanında durur.
- **Konfigürasyon koddan ayrı (12-factor):** Veritabanı şifresi gibi bilgiler kodun içinde değil, ortam değişkenlerinde (environment variables) tutulur. Aynı kod hem local'de hem sunucuda çalışır.

---

## 1. Sunucuya güvenli bağlanma ve ilk hazırlık

**Ne yapıyoruz:** Sunucuya SSH ile bağlanıp sistemi güncelliyoruz.
**Neden:** Yeni bir sunucu eski paketlerle gelir; güvenlik yamalarını almak için ilk iş güncellemektir.

```bash
# Kendi bilgisayarınızdan sunucuya bağlanın
ssh kullanici_adi@SUNUCU_IP

# Paket listesini güncelle ve yüklü paketleri en son sürüme çek
sudo apt update && sudo apt upgrade -y
```

> `sudo` = "süper kullanıcı (root) yetkisiyle çalıştır" demektir. Sistem geneline dokunan komutlar root yetkisi ister.

### root yerine normal kullanıcı (önemli profesyonel alışkanlık)

Eğer sunucuya `root` olarak bağlanıyorsanız, kendinize ayrı bir kullanıcı açın ve günlük işleri onunla yapın. `root` ile çalışmak, yanlış bir komutun tüm sistemi bozma riskini artırır.

```bash
# Yeni kullanıcı oluştur (örnek: deploy)
adduser deploy

# Bu kullanıcıya sudo (yönetici) yetkisi ver
usermod -aG sudo deploy

# Artık bu kullanıcıyla bağlanabilirsiniz: ssh deploy@SUNUCU_IP
```

---

## 2. Temel güvenlik: Firewall (UFW)

**Ne yapıyoruz:** Sadece izin verdiğimiz portları dışarıya açan bir güvenlik duvarı kuruyoruz.
**Neden:** Varsayılan olarak her port kapalı olmalı; sadece gerekli olanları (SSH, web) açarız. Bu, saldırı yüzeyini küçültür.

```bash
sudo apt install -y ufw

# Önce SSH'a izin ver (yoksa kendinizi sunucudan kilitlersiniz!)
sudo ufw allow OpenSSH

# Güvenlik duvarını aç
sudo ufw enable

# Durumu kontrol et
sudo ufw status
```

> **Dikkat:** `ufw enable` demeden ÖNCE mutlaka SSH'a izin verin. Vermezseniz bağlantınız kopunca tekrar giremezsiniz.

İlerleyen adımlarda web portlarını (`80`, `443`) da açacağız.

---

## 3. Swap alanı oluşturma (4 GB RAM için önemli)

**Ne yapıyoruz:** Disk üzerinde, RAM dolduğunda devreye giren bir "yedek bellek" (swap) alanı açıyoruz.
**Neden:** 4 GB RAM çoğu iş için yeter, ama ani yük anlarında (örneğin bir build veya ağır sorgu) RAM dolarsa sistem swap'a düşerek çökmeyi önler. Küçük sunucularda standart bir önlemdir.

```bash
# 2 GB'lık swap dosyası oluştur
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Sunucu yeniden başlasa da kalıcı olsun diye fstab'a ekle
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Kontrol et
free -h
```

---

## 4. Docker ve Docker Compose kurulumu

**Ne yapıyoruz:** Container teknolojisini kuruyoruz.
**Neden:** Docker, uygulamamızı ve veritabanını izole kutularda çalıştırmamızı sağlar. Docker Compose ise birden fazla container'ı (uygulama + veritabanı) tek bir dosyadan yönetmemizi sağlar.

Aşağıdaki, Docker'ın **resmi ve güncel** kurulum yöntemidir (Ubuntu'nun kendi `docker.io` paketi eski olabilir):

```bash
# Eski/çakışan sürümleri temizle
for pkg in docker.io docker-doc docker-compose podman-docker containerd runc; do sudo apt-get remove -y $pkg; done

# Gerekli araçlar
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings

# Docker'ın resmi GPG anahtarı (paketlerin gerçekten Docker'dan geldiğini doğrular)
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Docker deposunu sisteme ekle
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker'ı kur
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

**Kurulumdan sonra: kendinizi docker grubuna ekleyin**

```bash
# Böylece her docker komutunda 'sudo' yazmanız gerekmez
sudo usermod -aG docker $USER
```

> Bu komuttan sonra **oturumu kapatıp tekrar girmeniz** (logout/login veya yeniden SSH) gerekir, yoksa grup değişikliği aktif olmaz.

**Doğrulama:**

```bash
docker --version
docker compose version
docker run hello-world   # Test container'ı; "Hello from Docker!" görmelisiniz
```

> Not: Modern Docker'da komut `docker compose` (boşluklu, v2). Eski dokümanlardaki `docker-compose` (tireli, v1) artık kullanılmıyor.

---

## 5. Proje klasör yapısını oluşturma

**Ne yapıyoruz:** Sunucuda düzenli bir proje klasörü kuruyoruz.
**Neden:** Profesyonel kurulumlarda her şey belli bir düzende durur; "şu dosya neredeydi" karmaşası olmaz.

```bash
# Ana proje klasörü ve uygulama alt klasörü
mkdir -p ~/myapp/app
cd ~/myapp
```

Sonunda yapı şöyle olacak:

```
~/myapp/
├── docker-compose.yml     # Tüm servislerin orkestrasyonu
├── .env                   # Gizli bilgiler (şifreler) — ASLA git'e gitmez
└── app/
    ├── Dockerfile         # Spring Boot uygulamasını container'a çevirir
    └── app.jar            # Local'de build ettiğimiz uygulama
```

---

## 6. Gizli bilgileri `.env` dosyasında tutma

**Ne yapıyoruz:** Veritabanı kullanıcı adı/şifresini ayrı bir dosyaya koyuyoruz.
**Neden:** Şifreleri ne koda ne de Compose dosyasına gömeriz. `.env` dosyası gizli kalır (git'e gönderilmez) ve kolayca değiştirilebilir.

```bash
nano ~/myapp/.env
```

İçine şunları yazın (şifreyi **güçlü ve kendinize özel** seçin):

```env
POSTGRES_DB=appdb
POSTGRES_USER=appuser
POSTGRES_PASSWORD=BurayaCokGuclu_Bir_Sifre_2026!

# MinIO (dosya deposu) yönetici bilgileri
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=CokGuclu_Minio_Sifresi_2026!
```

> `nano` editöründe kaydetmek için: `Ctrl+O` → Enter → `Ctrl+X`.

---

## 7. Spring Boot'u dış konfigürasyona hazırlama

**Ne yapıyoruz:** Uygulamanın veritabanı ve dosya deposu bilgilerini koddan değil, ortam değişkenlerinden okumasını sağlıyoruz.
**Neden:** Aynı `.jar` dosyası hiç değişmeden hem local'de hem sunucuda çalışmalı. Tek değişen, ortam değişkenleri olmalı (12-factor app prensibi).

Bu projede `src/main/resources/application.yml` aşağıdaki gibi olmalı. Daha önce Supabase'e bakan `datasource` ve `supabase:` bloğu; artık kendi PostgreSQL'imize ve MinIO'muza bakıyor. Mail ve Firebase dış servis oldukları için aynen korunur:

```yaml
server:
  port: ${PORT:8080}

spring:
  datasource:
    # Sunucuda docker-compose'un geçtiği SPRING_DATASOURCE_* değişkenleri bunları ezer.
    # Aşağıdakiler LOCAL geliştirme varsayılanıdır.
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/appdb}
    username: ${SPRING_DATASOURCE_USERNAME:appuser}
    password: ${SPRING_DATASOURCE_PASSWORD:localsifre}
    driver-class-name: org.postgresql.Driver

  mail:
    host: smtp.gmail.com
    port: 587
    username: ereniridere7@gmail.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

  jpa:
    hibernate:
      # İlk kurulumda tablolar oluşsun diye 'update' (Bölüm 12, Yol B).
      # Yapı oturunca sunucuda SPRING_JPA_HIBERNATE_DDL_AUTO=validate yapın:
      # - validate: Hibernate tablolara dokunmaz, sadece uyumu kontrol eder. (üretim için ÖNERİLEN)
      # - update: Eksik tablo/kolon ekler (geliştirmede kullanışlı, üretimde riskli)
      # - create / create-drop: Tabloları SİLİP yeniden kurar — ÜRETİMDE ASLA
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

# Supabase Storage yerine kendi MinIO'muz (S3 uyumlu).
storage:
  endpoint: ${STORAGE_ENDPOINT:http://localhost:9000}
  access-key: ${STORAGE_ACCESS_KEY:minioadmin}
  secret-key: ${STORAGE_SECRET_KEY:minioadmin}
  bucket: ${STORAGE_BUCKET:komsum-images}
  region: ${STORAGE_REGION:us-east-1}
  path-style-access: true       # MinIO için zorunlu (S3 SDK'da forcePathStyle=true)

firebase:
  service-account-base64: ${FIREBASE_SERVICE_ACCOUNT_BASE64:}
```

> Spring Boot, `SPRING_DATASOURCE_URL` gibi büyük harfli ortam değişkenlerini otomatik olarak `spring.datasource.url`'e eşler. Compose'da bu değişkenleri verdiğimizde uygulama kendiliğinden okur, yml'deki varsayılanı ezer.

> **pom.xml — S3 bağımlılığı:** MinIO'ya bağlanmak için AWS S3 SDK gerekir. Supabase'in storage çağrısını silip bunu ekleyin:
>
> ```xml
> <dependency>
>     <groupId>software.amazon.awssdk</groupId>
>     <artifactId>s3</artifactId>
>     <version>2.31.0</version>
> </dependency>
> ```
>
> Ardından `storage.*` ayarlarını okuyan bir config sınıfıyla bir `S3Client` kurup (endpoint = `STORAGE_ENDPOINT`, `forcePathStyle(true)`), upload/download işlemlerini bu istemci üzerinden yaparsınız.

---

## 8. Uygulamayı build etmek (kendi bilgisayarınızda) ve sunucuya taşımak

**Ne yapıyoruz:** Jar dosyasını *kendi bilgisayarınızda* üretip sunucuya kopyalıyoruz.
**Neden:** Maven build işlemi anlık olarak çok RAM yer. 4 GB sunucuda build sırasında takılma yaşanabilir. Build'i güçlü olan kendi makinenizde yapıp, sunucuya sadece hazır `.jar`'ı göndermek hem hızlı hem güvenlidir. (İleride CI/CD ile bunu otomatikleştireceğiz.)

**Kendi bilgisayarınızda (sunucuda değil):**

```bash
# Projenizin kök dizininde
mvn clean package -DskipTests

# Üretilen jar genelde target/ klasöründedir, örn:
# target/myapp-0.0.1-SNAPSHOT.jar

# Jar'ı sunucuya kopyala (scp = secure copy)
scp target/myapp-0.0.1-SNAPSHOT.jar deploy@SUNUCU_IP:~/myapp/app/app.jar
```

> Jar'ı sunucuda `app.jar` adıyla sabitliyoruz ki Dockerfile her seferinde aynı ismi bulsun.

---

## 9. Uygulama için Dockerfile yazmak

**Ne yapıyoruz:** Spring Boot jar'ını bir Docker image'ına (çalıştırılabilir kutu kalıbı) dönüştüren tarifi yazıyoruz.
**Neden:** Container'ın içinde sadece Java çalışma ortamı (JRE) ve bizim jar'ımız bulunsun istiyoruz — gereksiz hiçbir şey yok, hafif ve taşınabilir.

Sunucuda `~/myapp/app/Dockerfile` dosyasını oluşturun:

```bash
nano ~/myapp/app/Dockerfile
```

İçeriği:

```dockerfile
# Sadece Java çalıştırma ortamı içeren hafif resmi imaj (derleme araçları yok)
FROM eclipse-temurin:21-jre

# Container içindeki çalışma klasörü
WORKDIR /app

# Local'de build ettiğimiz jar'ı container içine kopyala
COPY app.jar app.jar

# Uygulamanın dinlediği port (bilgilendirme amaçlı)
EXPOSE 8080

# 4 GB RAM'i aşmamak için JVM'e bellek sınırı veriyoruz
# Container başlayınca çalışacak komut:
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50.0", "-jar", "app.jar"]
```

> `MaxRAMPercentage=50.0`: JVM, container'a verilen belleğin en fazla %50'sini kullanır. Veritabanına ve sisteme yer kalsın diye.

---

## 10. Her şeyi birleştiren `docker-compose.yml`

**Ne yapıyoruz:** Veritabanı ve uygulamayı tek bir dosyada tanımlayıp birbirine bağlıyoruz.
**Neden:** Tek komutla (`docker compose up`) ikisini birden, doğru sırada ve doğru ağ ayarlarıyla ayağa kaldırmak için.

```bash
nano ~/myapp/docker-compose.yml
```

İçeriği:

```yaml
services:
  # --- Veritabanı servisi ---
  db:
    image: postgres:16
    container_name: myapp-db
    restart: unless-stopped          # Çökerse/sunucu yeniden başlarsa otomatik kalk
    environment:
      POSTGRES_DB: ${POSTGRES_DB}     # Değerler .env dosyasından gelir
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data   # Veri kalıcı olsun
    networks:
      - backend
    healthcheck:                      # Veritabanı gerçekten hazır mı kontrol et
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    # DİKKAT: Burada 'ports' YOK — veritabanı internete kapalı, sadece app erişir.

  # --- Uygulama servisi ---
  app:
    build: ./app                      # ./app/Dockerfile kullanılarak image kurulur
    container_name: myapp-app
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy    # DB hazır olmadan app başlamasın
    environment:
      # 'db' = veritabanı servisinin adı; Docker bunu otomatik hostname yapar
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      # MinIO (dosya deposu) bağlantı bilgileri — uygulamanız bunlarla S3'e bağlanır
      STORAGE_ENDPOINT: http://minio:9000
      STORAGE_ACCESS_KEY: ${MINIO_ROOT_USER}
      STORAGE_SECRET_KEY: ${MINIO_ROOT_PASSWORD}
      STORAGE_BUCKET: komsum-images
    ports:
      - "8080:8080"                   # Dışarıdan 8080'e gelen, container'ın 8080'ine gider
    networks:
      - backend

  # --- Dosya deposu (Supabase Storage yerine) ---
  minio:
    image: minio/minio
    container_name: myapp-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - miniodata:/data               # Dosyalar kalıcı olsun
    ports:
      - "9001:9001"                   # Sadece yönetim arayüzü dışarı açık
    networks:
      - backend
    # DİKKAT: S3 portu (9000) bilerek dışarı açılmadı; app ona 'http://minio:9000' ile içeriden ulaşır.

# Kalıcı veri alanı (container silinse de durur)
volumes:
  pgdata:
  miniodata:

# Servislerin konuştuğu özel, izole ağ
networks:
  backend:
```

**Burada öğrenmeniz gereken kilit noktalar:**

- **`app`, veritabanına `db` ismiyle ulaşır** (`jdbc:postgresql://db:5432/...`). IP adresi yazmamıza gerek yok; Docker, servis adını otomatik olarak ağ içinde bir hostname'e çevirir.
- **`db` servisinde `ports` yok:** Veritabanı dış dünyaya kapalı. Sadece `backend` ağındaki `app` ona erişebilir. **Bu, güvenliğin kalbidir.**
- **`depends_on` + `healthcheck`:** Uygulama, veritabanı *gerçekten hazır olana kadar* beklemez başlayıp hata vermesin diye.
- **`restart: unless-stopped`:** Sunucu yeniden başlasa bile servisler kendiliğinden kalkar. (Bu yüzden Docker kullanırken ayrıca `systemd` servisi yazmaya gerek kalmaz.)
- **`minio` servisi Supabase Storage'ın yerini alır:** S3 protokolünü konuştuğu için uygulamanız aynı S3 mantığıyla dosya yükler/indirir. Uygulama ona `http://minio:9000` ile içeriden ulaşır; sadece yönetim arayüzü (9001) dışarı açıktır.

---

## 11. Çalıştırma ve kontrol

**Ne yapıyoruz:** Tüm yapıyı ayağa kaldırıp çalıştığını doğruluyoruz.

```bash
cd ~/myapp

# Image'ları kur ve container'ları arka planda (-d = detached) başlat
docker compose up -d --build

# Çalışan servisleri gör
docker compose ps

# Uygulama loglarını canlı izle (Ctrl+C ile çıkarsınız, container durmaz)
docker compose logs -f app
```

**Doğrulama:**

```bash
# Uygulama ayakta mı? (sunucu içinden test)
curl http://localhost:8080/   # veya projenizin bir endpoint'i
```

**Faydalı günlük komutlar:**

```bash
docker compose stop          # Servisleri durdur (veri kalır)
docker compose start         # Tekrar başlat
docker compose down          # Container'ları kaldır (volume/veri KALIR)
docker compose logs db       # Veritabanı loglarına bak
docker stats                 # Hangi container ne kadar RAM/CPU kullanıyor
```

---

## 12. Supabase'den tablo yapısını (şema) taşıma

**Ne yapıyoruz:** Supabase'deki tablo yapısını kendi PostgreSQL'imize aktarıyoruz.
**Neden:** Artık veritabanımız sunucuda. Tablolar olmadan uygulama çalışamaz. Veri boş/az olduğu için sadece *yapıyı* (tablolar, kolonlar, kısıtlar) taşımak yeterli.

İki yöntem var; durumunuza göre birini seçin.

### Yol A — Supabase'den şemayı dışa aktar (en sadık yöntem)

Supabase panelinde elle index/constraint/trigger eklediyseniz bu yöntem her şeyi birebir taşır.

1. Supabase panelinde **Connect** butonuna basıp bağlantı bilgilerinizi alın (host, kullanıcı, şifre).
2. Kendi bilgisayarınızda, sürüm uyumsuzluğunu önlemek için `pg_dump`'ı Docker üzerinden çalıştırın:

```bash
docker run --rm postgres:16 pg_dump \
  --schema-only --schema=public --no-owner --no-privileges \
  "postgresql://postgres.[PROJE_REF]:[SIFRE]@aws-[BOLGE].pooler.supabase.com:5432/postgres" \
  > schema.sql
```

> - `--schema-only`: sadece yapıyı al, veriyi alma.
> - `--schema=public`: yalnızca sizin tablolarınızın olduğu `public` şemasını al; Supabase'in iç şemalarını (auth, storage vb.) alma.
> - `--no-owner --no-privileges`: Supabase'e özgü rol/yetki satırlarını atla ki kendi DB'nizde hata vermesin.

3. `schema.sql` dosyasını sunucuya kopyalayın ve kendi PostgreSQL'inize yükleyin:

```bash
# Dosyayı sunucuya gönder (kendi bilgisayarınızdan)
scp schema.sql deploy@SUNUCU_IP:~/myapp/

# Sunucuda, ~/myapp içindeyken yükle
docker exec -i myapp-db psql -U appuser -d appdb < schema.sql
```

### Yol B — Hibernate tabloları kursun (dev için en hızlısı)

Entity sınıflarınız zaten tabloları tanımlıyorsa hiçbir dışa aktarmaya gerek yok. Bu projede `application.yml`'de `ddl-auto` zaten ortam değişkeninden okunuyor (`SPRING_JPA_HIBERNATE_DDL_AUTO`), o yüzden sunucuda dosyaya dokunmadan, sadece değişkeni değiştirip uyguluyoruz:

1. İlk kurulumda `.env` dosyasına şunu ekleyin (tablolar otomatik oluşsun diye):

```env
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

> Not: `docker-compose.yml`'de `app` servisinde `SPRING_JPA_HIBERNATE_DDL_AUTO: validate` satırı var. Yol B'yi seçtiyseniz bu satırı `${SPRING_JPA_HIBERNATE_DDL_AUTO}` yapıp değeri `.env`'den yönetmek en temizidir.

2. Uygulamayı boş veritabanına karşı bir kez çalıştırın (`docker compose up -d --build app`) — Hibernate tabloları otomatik oluşturur.
3. Yapı oturunca güvenli ayara dönün ve yeniden başlatın:

```env
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

```bash
docker compose up -d app
```

> **Hangisini seçmeli?** Supabase'de panelden elle yapısal eklemeler (index/trigger vb.) yaptıysanız **Yol A** daha güvenli. Tüm yapı entity'lerinizde tanımlıysa **Yol B** en pratik. Dev aşamasında ikisi de risksiz.

---

## 13. Supabase Storage'dan MinIO'ya geçiş

**Ne yapıyoruz:** Dosya deposunu Supabase Storage'dan, sunucumuzdaki MinIO'ya taşıyoruz.
**Neden:** Supabase'i tamamen bırakıyoruz. MinIO, S3 protokolünü konuştuğu için hem dosyalarınızı tutar hem de uygulamanız neredeyse aynı kodla çalışmaya devam eder.

MinIO servisi `docker-compose.yml`'ye zaten eklendi (Bölüm 10) ve `docker compose up -d` ile ayağa kalktı. Şimdi içini hazırlayıp dosyaları taşıyacağız.

### 13.1 — Bucket (klasör) oluşturma

1. Tarayıcıdan `http://SUNUCU_IP:9001` adresine gidin.
2. `.env`'deki `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` ile giriş yapın.
3. **Create Bucket** deyip Supabase'dekiyle aynı isimde bir bucket oluşturun: `komsum-images`. Bu isim, `docker-compose.yml`'deki `STORAGE_BUCKET` değeriyle aynı olmalı.

> Güvenlik için MinIO yönetim arayüzünü (9001) sadece kendi IP'nize açmak isterseniz: `sudo ufw allow from SIZIN_IP to any port 9001 proto tcp`. Yoksa şimdilik test için `sudo ufw allow 9001/tcp`.

### 13.2 — Mevcut dosyaları Supabase'den indirip MinIO'ya yükleme

Dev aşamasında az dosya olduğu için en pratik yol:

1. Supabase panelinde **Storage** bölümünden dosyaları bilgisayarınıza indirin.
2. MinIO arayüzünde (9001) ilgili bucket'a girip dosyaları sürükleyip yükleyin.

Çok sayıda dosya varsa `rclone` ile iki S3 deposu arasında toplu kopyalama yapılabilir; ama birkaç dosya için yukarıdaki elle yöntem yeterlidir.

### 13.3 — Spring Boot'un storage ayarını MinIO'ya çevirme

Uygulamanız şu an Supabase Storage'a bakıyor. Bunu MinIO'ya yönlendireceğiz. MinIO S3 uyumlu olduğu için Java tarafında **AWS S3 SDK** kullanmak en temiz yoldur.

`docker-compose.yml`'de uygulamaya zaten şu değişkenleri geçtik (Bölüm 10):

```
STORAGE_ENDPOINT=http://minio:9000     # MinIO'ya sunucu içinden erişim
STORAGE_ACCESS_KEY=<MINIO_ROOT_USER>
STORAGE_SECRET_KEY=<MINIO_ROOT_PASSWORD>
STORAGE_BUCKET=komsum-images
```

S3 istemcisini kurarken dikkat edilecek iki MinIO özelliği:

- **`forcePathStyle = true`** (yol-stili adresleme) — MinIO bunu gerektirir.
- **Region** önemli değil; `us-east-1` gibi bir değer verilebilir.

> Kodunuzda dosya yüklemeyi şu an Supabase'in kendi çağrısıyla mı yoksa S3 SDK ile mi yapıyorsunuz bilmiyorum. S3 SDK'ya geçişte değişen tek şey: endpoint (`http://minio:9000`), erişim anahtarları ve `forcePathStyle`. Mevcut storage kodunuzu paylaşırsanız, MinIO'ya göre birebir nasıl uyarlanacağını ekleyebilirim.

---

## 14. Dışarıya açma (Firewall portu)

**Ne yapıyoruz:** 8080 portunu test amaçlı internete açıyoruz.

```bash
sudo ufw allow 8080/tcp
```

Artık tarayıcıdan `http://SUNUCU_IP:8080` ile erişebilirsiniz.

> **Profesyonel not:** Uygulamayı doğrudan `8080` ile internete açmak geçici/test için uygundur. Asıl üretimde bunu kapatıp, trafiği bir sonraki adımdaki Nginx üzerinden (80/443) geçiririz. O zaman `sudo ufw delete allow 8080/tcp` ile bu portu tekrar kapatırsınız.

---

## 15. (İleri seviye) Nginx reverse proxy + domain + ücretsiz SSL

**Ne yapıyoruz:** Önüne bir Nginx koyup, kullanıcıların `https://siteniz.com` ile (port yazmadan, şifreli) erişmesini sağlıyoruz.
**Neden:** Profesyonel kurulumlarda uygulama hiçbir zaman doğrudan internete bakmaz. Nginx; SSL'i, alan adını ve gelen trafiği yönetir; uygulama arkada güvende durur.

```bash
# Nginx kur
sudo apt install -y nginx

# Web portlarını aç, eski test portunu kapat
sudo ufw allow 'Nginx Full'   # 80 ve 443
sudo ufw delete allow 8080/tcp
```

Nginx yapılandırması:

```bash
sudo nano /etc/nginx/sites-available/myapp
```

```nginx
server {
    listen 80;
    server_name siteniz.com www.siteniz.com;   # Kendi domaininiz

    # Canlı mesaj akışı (SSE). Buffering KAPALI olmalı, aksi halde olaylar
    # nginx tarafından tamponlanıp anlık iletilmez. Uzun ömürlü bağlantı için
    # read timeout yüksek tutulur.
    location /api/chats/stream {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 3600s;
    }

    location / {
        proxy_pass http://localhost:8080;       # İçerideki Spring Boot'a yönlendir
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Yapılandırmayı aktive et
sudo ln -s /etc/nginx/sites-available/myapp /etc/nginx/sites-enabled/
sudo nginx -t          # Söz dizimini test et
sudo systemctl reload nginx
```

**Ücretsiz SSL (Let's Encrypt):** Önce domaininizin DNS'ini sunucu IP'sine yönlendirin, sonra:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d siteniz.com -d www.siteniz.com
```

Certbot, sertifikayı kurar ve otomatik yenileme ayarlar. Artık siteniz `https://` ile çalışır.

---

## 16. Kod değişince yeniden dağıtım (deploy döngüsü)

Bir şirkette günlük rutin budur. Kodu değiştirdiniz, sunucuya nasıl alırsınız:

```bash
# 1) Kendi bilgisayarınızda yeni jar'ı üret
mvn clean package -DskipTests

# 2) Sunucuya kopyala (eskisinin üzerine yazar)
scp target/myapp-0.0.1-SNAPSHOT.jar deploy@SUNUCU_IP:~/myapp/app/app.jar

# 3) Sunucuda sadece uygulamayı yeniden kur ve başlat (veritabanına dokunmadan)
cd ~/myapp
docker compose up -d --build app
```

---

## 17. Veritabanı yedeği alma

**Ne yapıyoruz:** Düzenli yedek alıyoruz.
**Neden:** Veri en değerli şeydir. Yedeksiz çalışmak profesyonel ortamda kabul edilemez.

```bash
# Anlık yedek al (sunucuda bir dosyaya)
docker exec myapp-db pg_dump -U appuser appdb > ~/yedek_$(date +%F).sql

# Geri yükleme (gerektiğinde)
cat ~/yedek_2026-06-16.sql | docker exec -i myapp-db psql -U appuser -d appdb
```

> İleri adım: Bu komutu `cron` ile her gece otomatik çalıştırıp yedekleri uzak bir depoya göndermek.

---

## 18. Buradan sonra — profesyonel evrim yol haritası

Temeli kurduktan sonra şirketlerin gittiği yön:

1. **Git deposu:** Kodu GitHub/GitLab'a koymak. `.env` ve `app.jar` dosyalarını `.gitignore`'a ekleyin (gizli/üretilen dosyalar repoya girmez).
2. **CI/CD (GitHub Actions):** `git push` yapınca jar'ın otomatik build edilip sunucuya dağıtılması. Manuel `scp`/build adımları ortadan kalkar.
3. **Container Registry:** Image'ı Docker Hub veya GitHub Container Registry'e push edip sunucuda sadece `pull` etmek. (Build'i sunucudan tamamen kaldırır.)
4. **İzleme (monitoring):** Uygulama ve sunucu sağlığını izlemek için araçlar (örn. Prometheus + Grafana, veya basitçe Uptime Kuma).
5. **Loglama:** Logları merkezi bir yerde toplamak.

---

## Hızlı kontrol listesi

- [ ] Sunucu güncel, normal kullanıcı + sudo ayarlı
- [ ] UFW açık, sadece gerekli portlar izinli
- [ ] Swap eklendi
- [ ] Docker + Compose kurulu, `hello-world` çalışıyor
- [ ] `~/myapp` klasör yapısı hazır
- [ ] `.env` dosyasında güçlü şifreler (Postgres + MinIO)
- [ ] Spring Boot ortam değişkenlerinden config okuyor, `ddl-auto=validate`
- [ ] Jar local'de build edilip sunucuya kopyalandı
- [ ] `docker compose up -d --build` ile db + app + minio container'ları ayakta
- [ ] Supabase'den tablo yapısı taşındı (Yol A veya B)
- [ ] MinIO'da bucket oluşturuldu, dosyalar Supabase'den taşındı
- [ ] Spring Boot storage ayarı MinIO'ya çevrildi (`http://minio:9000`, forcePathStyle)
- [ ] Uygulama veritabanına ve MinIO'ya bağlanıyor (loglarda hata yok)
- [ ] (İleri) Nginx + domain + SSL kurulu
- [ ] Veritabanı yedeği test edildi