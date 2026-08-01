INSERT INTO inventory_item (product_id, available_qty, reserved_qty) VALUES
    ('PROD-001', 50, 0),
    ('PROD-002', 30, 0),
    ('PROD-003', 5, 0),
    ('PROD-004', 100, 0)
ON CONFLICT (product_id) DO NOTHING;
