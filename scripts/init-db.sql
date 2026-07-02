-- ============================================================
-- StowFlow — Créer la base PostgreSQL (Option A, recommandée)
-- ============================================================
--
-- Où exécuter : connecté à la base "postgres" (pgAdmin, DBeaver, psql…)
--
-- Ensuite : démarrez stowflow-api — Hibernate (ddl-auto: update) crée les
-- tables et DataSeeder insère les données de démo.
--
-- Ne pas exécuter tout le dépôt d'un coup : ce fichier ne contient que
-- CREATE DATABASE. Le schéma manuel est dans init-db-schema.sql (Option B).
-- ============================================================

CREATE DATABASE stowflow
  WITH ENCODING 'UTF8'
       LC_COLLATE = 'French_France.1252'
       LC_CTYPE = 'French_France.1252'
       TEMPLATE = template0;

-- Si CREATE DATABASE échoue sur LC_COLLATE (Linux / Docker), utiliser :
-- CREATE DATABASE stowflow;
