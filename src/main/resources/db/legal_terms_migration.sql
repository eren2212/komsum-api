-- =============================================================================
--  SelamKomşu — Kullanım Koşulları Migration (MANUEL RUNBOOK)
--  Projede Flyway/Liquibase YOK; şema ddl-auto=update ile yönetiliyor.
--  Bu dosya, YENİ BACKEND SÜRÜMÜ DEPLOY EDİLMEDEN ÖNCE, veritabanında
--  ELLE çalıştırılır. Birden fazla kez çalıştırmak güvenlidir (idempotent).
--
--  Sunucuda çalıştırma örneği (servis/kullanıcı/db adlarını kendi
--  docker-compose.yml ve .env dosyana göre düzelt):
--     docker compose exec -T db psql -U appuser -d appdb < legal_terms_migration.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- NEDEN GEREKLİ?
-- Hibernate, @Enumerated(EnumType.STRING) kolonlar için tabloyu ilk oluştururken
--     CHECK (type IN ('KVKK','AYDINLATMA_METNI'))
-- biçiminde bir kısıt koyar. ddl-auto=update bu kısıta yeni enum değerini
-- EKLEMEZ. LegalDocumentType'a KULLANIM_KOSULLARI eklendiği için, bu adım
-- çalıştırılmadan deploy edilirse LegalDocumentSeeder açılışta
--     "violates check constraint legal_documents_type_check"
-- hatasıyla düşer ve UYGULAMA AÇILMAZ. Aynı sorun, kayıtta onay yazılan
-- user_consents.document_type kolonu için de geçerlidir.
--
-- Kısıt adı ortama göre değişebileceği için ada güvenmiyoruz: ilgili kolondaki
-- CHECK kısıtlarını katalogdan bulup kaldırıyor, güncel değer listesiyle
-- yeniden ekliyoruz.
-- -----------------------------------------------------------------------------

BEGIN;

-- ADIM 1 — legal_documents.type
DO $$
DECLARE c record;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'legal_documents'::regclass
          AND con.contype = 'c'
          AND att.attname = 'type'
    LOOP
        EXECUTE format('ALTER TABLE legal_documents DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE legal_documents
    ADD CONSTRAINT legal_documents_type_check
    CHECK (type IN ('KVKK', 'AYDINLATMA_METNI', 'KULLANIM_KOSULLARI'));

-- ADIM 2 — user_consents.document_type
DO $$
DECLARE c record;
BEGIN
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'user_consents'::regclass
          AND con.contype = 'c'
          AND att.attname = 'document_type'
    LOOP
        EXECUTE format('ALTER TABLE user_consents DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE user_consents
    ADD CONSTRAINT user_consents_document_type_check
    CHECK (document_type IN ('KVKK', 'AYDINLATMA_METNI', 'KULLANIM_KOSULLARI'));

COMMIT;

-- -----------------------------------------------------------------------------
-- DOĞRULAMA (her iki tabloda da üç değerli tek bir kısıt görünmeli)
-- -----------------------------------------------------------------------------
SELECT conrelid::regclass AS tablo, conname, pg_get_constraintdef(oid) AS tanim
FROM pg_constraint
WHERE conrelid IN ('legal_documents'::regclass, 'user_consents'::regclass)
  AND contype = 'c';


-- =============================================================================
--  >>> Bu noktada yeni backend'i deploy et (git pull + docker compose up -d --build). <<<
--  LegalDocumentSeeder açılışta:
--    - Aydınlatma ve Açık Rıza metinlerinin v2'sini ekler, v1'leri pasife alır
--      (v1 satırları SİLİNMEZ: eski kullanıcıların onay kaydı onlara işaret eder),
--    - Kullanım Koşulları v1'i ekler.
--  Doğrula — yürürlükte tam üç metin olmalı:
--    SELECT type, version, active, title FROM legal_documents ORDER BY type, version;
-- =============================================================================
