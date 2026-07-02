-- ============================================================
-- StowFlow — Réinitialiser la base (corrige conflit schéma manuel / JPA)
-- Exécuter connecté à "postgres" (pgAdmin, DBeaver, psql…)
-- ============================================================

-- Fermer les connexions actives
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname IN ('stowflow', 'stocki', 'StowFlow') AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS stowflow;
DROP DATABASE IF EXISTS stocki;
DROP DATABASE IF EXISTS "StowFlow";

CREATE DATABASE stowflow;

-- Ne PAS exécuter init-db-schema.sql ensuite.
-- Relancer l'API : Hibernate créera les tables + DataSeeder les remplira.
