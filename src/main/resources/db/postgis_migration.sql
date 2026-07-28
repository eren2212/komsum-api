-- =============================================================================
--  Komşum — PostGIS Spatial Migration (MANUEL RUNBOOK)
--  Projede Flyway/Liquibase YOK; şema ddl-auto=update ile yönetiliyor.
--  Bu dosyadaki adımlar Supabase SQL editöründe ELLE çalıştırılır.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- ADIM 0 — PostGIS extension (UYGULAMA İLK AÇILIŞINDAN ÖNCE çalıştır!)
-- Bu olmadan Hibernate'in geometry(Point,4326) kolonu oluşturma DDL'i patlar.
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS postgis;

-- Doğrulama (sürüm dönmeli):
-- SELECT postgis_full_version();


-- =============================================================================
--  >>> Bu noktada uygulamayı bir kez AYAĞA KALDIR. <<<
--  ddl-auto=update şu kolonları ekler:
--     events.geo_location           geometry(Point,4326)
--     merchant_profiles.geo_location geometry(Point,4326)
--  Açılış logunda DDL hatasız geçmeli. Doğrula:
--     \d events
--     \d merchant_profiles
-- =============================================================================


-- -----------------------------------------------------------------------------
-- ADIM 5a — Veri backfill (yalnızca events)
-- Eski Double latitude/longitude kolonlarındaki veriyi geo_location'a taşır.
-- (merchant_profiles'ta eski koordinat YOKTU → backfill yapılamaz; mevcut
--  esnaflar profil güncelleyip konum girene kadar SPONSORED radius'ta görünmez.)
-- -----------------------------------------------------------------------------
UPDATE events
SET geo_location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)  -- DİKKAT: (lng, lat)
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL
  AND geo_location IS NULL;


-- -----------------------------------------------------------------------------
-- ADIM 5b — GiST functional index'ler (PERFORMANS İÇİN ŞART)
-- Sorgular geo_location::geography üzerinden metre bazlı ST_DWithin çalıştırır;
-- index de aynı ifade üzerinde olmalı ki planner kullanabilsin (Seq Scan yerine).
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_events_geo_location_geog
  ON events USING GIST ((geo_location::geography));

CREATE INDEX IF NOT EXISTS idx_merchant_geo_location_geog
  ON merchant_profiles USING GIST ((geo_location::geography));

-- "Gerekli Kişiler / Ustalar" (service_provider_profiles) tablosu da aynı şekilde
-- geometry(Point,4326) geo_location taşır. İleride "yakındaki ustalar" sorgusu
-- metre bazlı ST_DWithin çalıştıracağı için index aynı ifade üzerinde olmalı.
CREATE INDEX IF NOT EXISTS idx_service_provider_geo_location_geog
  ON service_provider_profiles USING GIST ((geo_location::geography));

-- Roomio (roomio_profiles) tablosu da aynı şekilde geometry(Point,4326) geo_location
-- taşır. Aday akışı MAHALLE değil YARIÇAP (ST_DWithin, metre) bazlı filtre yapar,
-- bu yüzden index burada da şart. Tablo ddl-auto=update ile otomatik oluşur —
-- uygulamayı bir kez ayağa kaldırdıktan sonra bu index'i çalıştır.
CREATE INDEX IF NOT EXISTS idx_roomio_profiles_geo_location_geog
  ON roomio_profiles USING GIST ((geo_location::geography));

-- (Opsiyonel) İstatistikleri tazele:
-- ANALYZE events;
-- ANALYZE merchant_profiles;


-- -----------------------------------------------------------------------------
-- ADIM 5c — (OPSİYONEL, backfill DOĞRULANDIKTAN SONRA) eski kolonları düşür.
-- ddl-auto=update bu kolonları otomatik düşürmez; orphan kalırlar (zararsız).
-- Geri dönüşü olmadığı için backfill'i kontrol etmeden ÇALIŞTIRMA.
-- -----------------------------------------------------------------------------
-- ALTER TABLE events DROP COLUMN IF EXISTS latitude;
-- ALTER TABLE events DROP COLUMN IF EXISTS longitude;


-- =============================================================================
--  DOĞRULAMA SORGULARI (performans + indeks kullanımı)
-- =============================================================================
-- Index kullanılıyor mu? Çıktıda "Index Scan ... idx_events_geo_location_geog"
-- görülmeli (Seq Scan DEĞİL):
--
-- EXPLAIN ANALYZE
-- SELECT e.id FROM events e
-- WHERE e.is_active = true AND e.geo_location IS NOT NULL
--   AND ST_DWithin(e.geo_location::geography,
--         ST_SetSRID(ST_MakePoint(28.9784, 41.0082), 4326)::geography, 2000);
