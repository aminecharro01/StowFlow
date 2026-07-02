-- StowFlow baseline schema (from scripts/init-db-schema.sql)

CREATE TABLE IF NOT EXISTS tenants (
  id   BIGSERIAL PRIMARY KEY,
  slug VARCHAR(64)  NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(32)  NOT NULL,
  tenant_id     BIGINT REFERENCES tenants(id),
  supplier_id   BIGINT,
  enabled       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS articles (
  id               BIGSERIAL PRIMARY KEY,
  tenant_id        BIGINT NOT NULL REFERENCES tenants(id),
  sku              VARCHAR(64)  NOT NULL,
  name             VARCHAR(512) NOT NULL,
  category         VARCHAR(128),
  unit_of_measure  VARCHAR(32)  DEFAULT 'unit',
  purchase_price   NUMERIC(14,2) DEFAULT 0,
  sale_price       NUMERIC(14,2) DEFAULT 0,
  min_threshold    INTEGER NOT NULL DEFAULT 0,
  max_threshold    INTEGER NOT NULL DEFAULT 2147483647,
  quantity_on_hand INTEGER NOT NULL DEFAULT 0,
  description      VARCHAR(4000),
  archived         BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (tenant_id, sku)
);

CREATE TABLE IF NOT EXISTS suppliers (
  id                   BIGSERIAL PRIMARY KEY,
  tenant_id            BIGINT NOT NULL REFERENCES tenants(id),
  name                 VARCHAR(255) NOT NULL,
  contact_email        VARCHAR(255),
  country              VARCHAR(128),
  lead_time_days       INTEGER NOT NULL DEFAULT 7,
  status               VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  archived             BOOLEAN NOT NULL DEFAULT FALSE,
  linked_product_count INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE app_users
  ADD CONSTRAINT fk_app_users_supplier
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

CREATE TABLE IF NOT EXISTS purchase_orders (
  id                    BIGSERIAL PRIMARY KEY,
  tenant_id             BIGINT NOT NULL REFERENCES tenants(id),
  order_number          VARCHAR(64) NOT NULL,
  supplier_id           BIGINT NOT NULL REFERENCES suppliers(id),
  order_date            DATE NOT NULL,
  status                VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  total_amount          NUMERIC(14,2) DEFAULT 0,
  line_item_count       INTEGER NOT NULL DEFAULT 0,
  supplier_confirmed_at TIMESTAMP,
  tenant_received_at    TIMESTAMP,
  received_by           VARCHAR(255),
  UNIQUE (tenant_id, order_number)
);

CREATE TABLE IF NOT EXISTS purchase_order_lines (
  id                 BIGSERIAL PRIMARY KEY,
  purchase_order_id  BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
  article_id         BIGINT NOT NULL REFERENCES articles(id),
  quantity           INTEGER NOT NULL,
  quantity_received  INTEGER NOT NULL DEFAULT 0,
  unit_price         NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS replenishment_requests (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenants(id),
  article_id   BIGINT NOT NULL REFERENCES articles(id),
  supplier_id  BIGINT NOT NULL REFERENCES suppliers(id),
  quantity     INTEGER NOT NULL,
  status       VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  note         VARCHAR(512),
  requested_by VARCHAR(255) NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  processed_by VARCHAR(255),
  processed_at TIMESTAMP,
  received_by  VARCHAR(255),
  received_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sales (
  id            BIGSERIAL PRIMARY KEY,
  tenant_id     BIGINT NOT NULL REFERENCES tenants(id),
  sale_number   VARCHAR(80) NOT NULL,
  created_at    TIMESTAMP NOT NULL,
  sold_by_email VARCHAR(255) NOT NULL,
  total_amount  NUMERIC(14,2) NOT NULL,
  line_count    INTEGER NOT NULL DEFAULT 0,
  note          VARCHAR(2000),
  UNIQUE (tenant_id, sale_number)
);

CREATE TABLE IF NOT EXISTS sale_lines (
  id            BIGSERIAL PRIMARY KEY,
  sale_id       BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
  article_id    BIGINT NOT NULL REFERENCES articles(id),
  sku_snapshot  VARCHAR(64)  NOT NULL DEFAULT '',
  name_snapshot VARCHAR(512) NOT NULL DEFAULT '',
  quantity      INTEGER NOT NULL,
  unit_price    NUMERIC(14,2) NOT NULL,
  line_total    NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stock_movements (
  id                       BIGSERIAL PRIMARY KEY,
  article_id               BIGINT NOT NULL REFERENCES articles(id),
  type                     VARCHAR(32) NOT NULL,
  quantity                 INTEGER NOT NULL,
  note                     VARCHAR(2000),
  created_at               TIMESTAMP NOT NULL,
  created_by               VARCHAR(255) NOT NULL,
  sale_line_id             BIGINT REFERENCES sale_lines(id),
  purchase_order_line_id   BIGINT REFERENCES purchase_order_lines(id),
  replenishment_request_id BIGINT REFERENCES replenishment_requests(id)
);

CREATE INDEX IF NOT EXISTS idx_articles_tenant ON articles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_tenant ON suppliers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_tenant ON purchase_orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_movements_article ON stock_movements(article_id);
