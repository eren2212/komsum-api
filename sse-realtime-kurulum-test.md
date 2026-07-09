# Canlı Mesajlaşma (SSE) — Sunucu Kurulumu ve Test Rehberi

Bu rehber, mesajlaşmanın **Supabase realtime** yerine **kendi sunucundaki SSE
akışı** ile çalışması için sunucuda ne yapman ve nasıl test etmen gerektiğini
adım adım anlatır.

> **Kısaca ne değişti?** Mesaj *gönderme* zaten REST ile backend'e gidiyordu.
> Artık mesaj *anlık teslimi* de senin sunucundan geliyor: backend
> `GET /api/chats/stream` adresinde bir SSE akışı açıyor, mesaj kaydedilince
> odadaki iki kullanıcıya da anında push ediyor. Supabase tamamen devreden çıktı.

---

## 0. Ön Koşullar (Kontrol Listesi)

- [ ] Sunucuda Docker ve veritabanı (PostgreSQL) zaten kurulu ve çalışıyor.
- [ ] Backend Docker imajı bu yeni kodla **yeniden build** edilecek.
- [ ] Sunucuda nginx reverse proxy kullanıyorsun (domain: `api.ereniridere.xyz`).
- [ ] Mobil uygulamanın `.env` dosyasında `EXPO_PUBLIC_API_URL=https://api.ereniridere.xyz/` ayarlı.

> Yeni bir ortam değişkeni (environment variable) **gerekmiyor.**

---

## 1. Güncel Kodu Sunucuya Al

Sunucuda projenin bulunduğu klasöre gir ve en güncel kodu çek:

```bash
cd /path/to/komsum        # backend klasörünün yolu
git pull                  # (kodu git ile yönetiyorsan)
```

> Eğer kodu git ile değil de elle (scp/rsync) atıyorsan, güncellenen dosyaların
> sunucuya kopyalandığından emin ol.

---

## 2. Backend Docker İmajını Yeniden Build Et ve Başlat

### Docker Compose kullanıyorsan
```bash
cd /path/to/komsum
docker compose build app          # imajı yeniden derle
docker compose up -d app          # yeni imajla yeniden başlat
```

### Tek başına `docker build` kullanıyorsan
```bash
cd /path/to/komsum
docker build -t komsum .
docker stop komsum-app && docker rm komsum-app      # eski container'ı durdur/sil
docker run -d --name komsum-app -p 8080:8080 \
  --env-file .env komsum                            # yeni container'ı başlat
```

> `--env-file` ve port eşlemesini kendi mevcut kurulumuna göre kullan; burada
> sadece **imajı yeniden build edip yeniden başlatman** önemli.

### Başladığını doğrula
```bash
docker ps                          # komsum container'ı "Up" görünmeli
docker logs -f komsum-app          # hata var mı diye logları izle (Ctrl+C ile çık)
```

Loglarda `Started ... in X seconds` benzeri bir satır görmelisin, hata (stack
trace) olmamalı.

---

## 3. nginx'e SSE İçin Ayar Ekle (ÇOK ÖNEMLİ)

SSE'nin çalışması için nginx'in bu adresi **tamponlamadan (buffering kapalı)**
geçirmesi gerekir. Aksi halde mesajlar nginx'te birikir, anlık gelmez.

### 3.1 nginx config dosyasını aç
```bash
sudo nano /etc/nginx/sites-available/myapp
```
> Dosya adı sende farklı olabilir (ör. `default` veya `api.ereniridere.xyz`).
> Hangi dosya olduğunu görmek için: `ls -l /etc/nginx/sites-enabled/`

### 3.2 `location /` bloğunun ÜSTÜNE şu bloğu ekle

`server { ... }` bloğunun içine, mevcut `location / { ... }` satırından **önce**
ekle (nginx daha özgül olan `/api/chats/stream` adresini önce eşler):

```nginx
    # Canlı mesaj akışı (SSE) — buffering KAPALI olmalı
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
```

> **Dikkat:** HTTPS kullanıyorsun (certbot). Certbot genelde `443` portu için
> ayrı bir `server { ... }` bloğu oluşturur. Bu `location` bloğunu **HTTPS'i
> (443) dinleyen server bloğuna** eklediğinden emin ol — mobil uygulama
> `https://` üzerinden bağlanıyor. İki ayrı server bloğun varsa (80 ve 443),
> 443'lü olana eklemen yeterli.

### 3.3 Söz dizimini test et ve nginx'i yeniden yükle
```bash
sudo nginx -t            # "syntax is ok" + "test is successful" görmelisin
sudo systemctl reload nginx
```

> `nginx -t` hata verirse, eklediğin bloğun süslü parantezlerini ve `server { }`
> bloğunun **içinde** olduğunu kontrol et.

---

## 4. Test — Adım Adım

### Test 1: SSE akışı tek başına çalışıyor mu? (curl ile)

Önce geçerli bir **access token**'a ihtiyacın var. İki yol:

**A) Login isteğiyle token al:**
```bash
curl -s -X POST https://api.ereniridere.xyz/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"SENIN_EMAIL","password":"SENIN_SIFRE"}'
```
Dönen cevaptaki `data.access_token` değerini kopyala.

**B) Mobil uygulamadan:** uygulama loglarında (Metro konsolu) `auth=Bearer ***`
satırından token görünür; ya da geçici olarak loglatabilirsin.

Şimdi SSE akışına bağlan:
```bash
curl -N -H "Authorization: Bearer BURAYA_TOKEN" \
  https://api.ereniridere.xyz/api/chats/stream
```

**Beklenen sonuç:**
- Komut **kapanmadan açık kalır** (normal curl gibi hemen geri dönmez).
- İlk anda şu satırı görürsün:
  ```
  event: connected
  data: ok
  ```
- Yaklaşık her **20 saniyede bir** `:ping` satırı düşer (heartbeat).

Bunları görüyorsan **akış çalışıyor.** (`Ctrl+C` ile çık.)

> ❌ Komut hemen geri dönüyor / boş kalıyorsa: token geçersiz olabilir (401),
> ya da nginx `proxy_buffering off` ayarı eksik/yanlış server bloğunda olabilir.

### Test 2: Canlı mesaj iki kullanıcı arasında akıyor mu?

curl penceresini açık bırak (Test 1'deki token **A kullanıcısına** ait olsun).
Başka bir terminalde, **B kullanıcısının** token'ı ile A'ya mesaj gönder.
(Önce A ve B arasında bir oda olmalı — uygulamadan sohbet başlatılmışsa vardır.
Oda ID'sini inbox'tan görebilirsin.)

```bash
curl -s -X POST https://api.ereniridere.xyz/api/chats/ODA_ID/messages \
  -H "Authorization: Bearer B_KULLANICI_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"merhaba canlı test"}'
```

**Beklenen sonuç:** Test 1'deki açık curl penceresine **anında** şöyle bir
olay düşer:
```
event: message
data: {"id":123,"chatRoomId":ODA_ID,"senderId":B_ID,"content":"merhaba canlı test","createdAt":"2026-..."}
```

Düşüyorsa **uçtan uca canlı teslim çalışıyor.** 🎉

### Test 3: Gerçek uygulamada (iki cihaz)

1. İki ayrı cihaz/emülatörde, iki farklı kullanıcı (A ve B) ile giriş yap.
2. İkisi de aynı sohbet odasını açsın.
3. A mesaj yazıp gönderince, **B'nin ekranında anında** belirmeli.
4. B sohbet ekranında **değilken** (mesajlar listesinde / başka sekmede), yeni
   mesaj gelince **inbox listesi güncellenmeli** ve **okunmamış (unread) rozeti
   artmalı.**

### Test 4: FCM bildirimleri hâlâ çalışıyor mu?

1. B kullanıcısının uygulamasını **tamamen kapat** (arka planda bile olmasın).
2. A, B'ye mesaj atsın.
3. B'nin cihazına **push bildirimi** düşmeli.

> SSE ile FCM **birbirinden bağımsız**: uygulama açıkken canlı akış (SSE),
> kapalıyken bildirim (FCM) devrede.

### Test 5: Yeniden bağlanma (reconnect)

1. Uygulamayı arka plana al, birkaç saniye bekle, tekrar öne getir — ya da
   cihazın internetini kapatıp aç.
2. Yeni bir mesaj geldiğinde yine **anında** görünmeli (akış otomatik yeniden
   bağlanır).

---

## 5. Sorun Giderme (Troubleshooting)

| Belirti | Olası Neden | Çözüm |
|--------|-------------|-------|
| `curl .../stream` hemen kapanıyor | Token geçersiz/expired (401) | Yeni token al (Test 1-A) |
| Akış açık ama mesaj düşmüyor | nginx `proxy_buffering off` eksik veya yanlış server bloğunda | Adım 3.2'yi kontrol et, `nginx -t` + `reload` |
| Mesaj ~1 dk sonra toplu geliyor | nginx tamponluyor | `proxy_buffering off; proxy_cache off;` ekli mi? |
| Bağlantı 1 dk'da bir kopuyor | `proxy_read_timeout` düşük | `proxy_read_timeout 3600s;` ekle |
| Uygulamada hiç canlı gelmiyor ama curl çalışıyor | Mobil token bağlanmıyor | Metro loglarına bak; çıkış/giriş yapıp tekrar dene |
| Backend açılmıyor | Kod/derleme hatası | `docker logs komsum-app` ile stack trace'e bak |

### Faydalı komutlar
```bash
docker logs -f komsum-app                 # backend logları (canlı)
sudo tail -f /var/log/nginx/error.log     # nginx hataları
sudo tail -f /var/log/nginx/access.log    # gelen istekler (stream isteğini gör)
docker ps                                 # container durumu
```

---

## 6. Özet Akış Şeması

```
A mesaj yazar
   │  POST /api/chats/{oda}/messages   (REST, JWT)
   ▼
Backend  ── mesajı DB'ye kaydeder
   ├─► MessageSentEvent → FCM push   (uygulama kapalıysa bildirim)
   └─► SSE: A'ya ve B'ye anında push (uygulama açıksa canlı mesaj)
                 │
                 ▼  GET /api/chats/stream  (SSE, JWT)
        nginx (buffering OFF) ──► Mobil uygulama → ekranda görünür
```
