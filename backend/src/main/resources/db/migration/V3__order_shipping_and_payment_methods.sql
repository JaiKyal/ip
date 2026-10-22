-- V3: shipping details on orders, and a fixed list of payment methods.
-- Payments are SIMULATED: UPI and CARD are approved by a fake gateway, COD is paid on delivery.
-- No card numbers, CVVs or UPI ids are ever stored.

ALTER TABLE orders
    ADD COLUMN shipping_name    VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN shipping_phone   VARCHAR(20)  NOT NULL DEFAULT '',
    ADD COLUMN shipping_address VARCHAR(300) NOT NULL DEFAULT '',
    ADD COLUMN shipping_city    VARCHAR(80)  NOT NULL DEFAULT '',
    ADD COLUMN shipping_pincode VARCHAR(10)  NOT NULL DEFAULT '';

ALTER TABLE orders
    ADD CONSTRAINT orders_payment_method_check CHECK (payment_method IN ('UPI', 'CARD', 'COD'));

ALTER TABLE payments
    ADD CONSTRAINT payments_method_check CHECK (payment_method IN ('UPI', 'CARD', 'COD'));
