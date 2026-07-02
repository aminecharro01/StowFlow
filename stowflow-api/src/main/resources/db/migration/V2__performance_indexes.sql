-- Performance indexes for list and dashboard queries

CREATE INDEX IF NOT EXISTS idx_articles_tenant_archived ON articles(tenant_id, archived);
CREATE INDEX IF NOT EXISTS idx_movements_created_at ON stock_movements(created_at);
CREATE INDEX IF NOT EXISTS idx_movements_article_created ON stock_movements(article_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON purchase_orders(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_created ON sales(tenant_id, created_at);
