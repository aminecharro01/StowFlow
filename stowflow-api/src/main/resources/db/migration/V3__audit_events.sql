-- Audit trail for admin traceability

CREATE TABLE IF NOT EXISTS audit_events (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenants(id),
  user_id      BIGINT REFERENCES app_users(id),
  action       VARCHAR(64)  NOT NULL,
  entity_type  VARCHAR(64)  NOT NULL,
  entity_id    BIGINT,
  payload_json TEXT,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_created ON audit_events(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_events(entity_type, entity_id);
