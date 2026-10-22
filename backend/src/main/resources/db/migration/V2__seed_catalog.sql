-- V2: demo catalog (3 categories, 15 products). Prices are in INR.
-- Images are stable placeholder URLs; replace them from the admin dashboard once real images exist.
-- A few products have low or zero stock on purpose, to exercise the low-stock report and
-- the out-of-stock display. Users and orders are NOT seeded here; the first admin is created
-- at startup from environment variables, never from a committed password hash.

INSERT INTO categories (name, description) VALUES
    ('Electronics',     'Audio, wearables and charging accessories'),
    ('Fashion',         'Everyday clothing, footwear and bags'),
    ('Home & Kitchen',  'Cookware, drinkware and home essentials');

INSERT INTO products (name, description, price, stock_quantity, image_url, category_id)
SELECT p.name, p.description, p.price, p.stock_quantity, p.image_url, c.id
FROM (VALUES
    ('Wireless Earbuds',            'Bluetooth 5.3 earbuds with a charging case and up to 24 hours of playback.',          1499.00,  40, 'https://placehold.co/600x400?text=Wireless+Earbuds',   'Electronics'),
    ('Bluetooth Speaker',           'Portable water-resistant speaker with deep bass and a 12 hour battery.',               1999.00,  25, 'https://placehold.co/600x400?text=Bluetooth+Speaker',  'Electronics'),
    ('Smart Watch',                 'Fitness tracker with heart-rate monitor, sleep tracking and a 1.7 inch display.',      2999.00,   5, 'https://placehold.co/600x400?text=Smart+Watch',        'Electronics'),
    ('Power Bank 10000mAh',         'Slim 10000mAh power bank with dual USB output and fast charging.',                     1299.00,  60, 'https://placehold.co/600x400?text=Power+Bank',         'Electronics'),
    ('USB-C Fast Charger 30W',      '30W wall charger with a USB-C port and short-circuit protection.',                      799.00,   0, 'https://placehold.co/600x400?text=USB-C+Charger',      'Electronics'),

    ('Men Cotton T-Shirt',          'Soft 100 percent cotton round-neck t-shirt, regular fit.',                              499.00, 120, 'https://placehold.co/600x400?text=Cotton+T-Shirt',     'Fashion'),
    ('Women Printed Kurti',         'Lightweight printed kurti with three-quarter sleeves.',                                 899.00,  45, 'https://placehold.co/600x400?text=Printed+Kurti',      'Fashion'),
    ('Running Shoes',               'Breathable mesh running shoes with a cushioned sole.',                                 1799.00,  30, 'https://placehold.co/600x400?text=Running+Shoes',      'Fashion'),
    ('Denim Jacket',                'Classic blue denim jacket with button closure and two chest pockets.',                 2199.00,   4, 'https://placehold.co/600x400?text=Denim+Jacket',       'Fashion'),
    ('Canvas Backpack',             'Durable 25 litre canvas backpack with a padded laptop sleeve.',                         999.00,  55, 'https://placehold.co/600x400?text=Canvas+Backpack',    'Fashion'),

    ('Stainless Steel Water Bottle','1 litre insulated bottle that keeps drinks cold for 24 hours and hot for 12.',          599.00,  80, 'https://placehold.co/600x400?text=Steel+Bottle',       'Home & Kitchen'),
    ('Non-stick Frying Pan',        '26 cm non-stick frying pan, induction compatible, with a heat-resistant handle.',      1199.00,  35, 'https://placehold.co/600x400?text=Frying+Pan',         'Home & Kitchen'),
    ('Ceramic Coffee Mug Set',      'Set of 4 ceramic mugs, 300 ml each, microwave and dishwasher safe.',                    699.00,  50, 'https://placehold.co/600x400?text=Coffee+Mug+Set',     'Home & Kitchen'),
    ('LED Desk Lamp',               'Foldable LED desk lamp with three brightness levels and a USB charging port.',          899.00,   3, 'https://placehold.co/600x400?text=LED+Desk+Lamp',      'Home & Kitchen'),
    ('Cotton Double Bedsheet',      'Double-bed cotton bedsheet with two pillow covers, machine washable.',                 1299.00,  28, 'https://placehold.co/600x400?text=Cotton+Bedsheet',    'Home & Kitchen')
) AS p (name, description, price, stock_quantity, image_url, category_name)
JOIN categories c ON c.name = p.category_name;
