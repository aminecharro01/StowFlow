-- Align sale_lines with JPA entity (POS snapshot fields)

ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS sku_snapshot VARCHAR(64);
ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS name_snapshot VARCHAR(512);
ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS line_total NUMERIC(14,2);

UPDATE sale_lines
SET sku_snapshot = COALESCE(sku_snapshot, ''),
    name_snapshot = COALESCE(name_snapshot, ''),
    line_total = COALESCE(line_total, unit_price * quantity)
WHERE sku_snapshot IS NULL OR name_snapshot IS NULL OR line_total IS NULL;

ALTER TABLE sale_lines ALTER COLUMN sku_snapshot SET DEFAULT '';
ALTER TABLE sale_lines ALTER COLUMN sku_snapshot SET NOT NULL;
ALTER TABLE sale_lines ALTER COLUMN name_snapshot SET DEFAULT '';
ALTER TABLE sale_lines ALTER COLUMN name_snapshot SET NOT NULL;
ALTER TABLE sale_lines ALTER COLUMN line_total SET DEFAULT 0;
ALTER TABLE sale_lines ALTER COLUMN line_total SET NOT NULL;
