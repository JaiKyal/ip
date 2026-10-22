-- V4: colour-coded placeholder images per category for the demo catalog (a data-only migration).
-- Only rows still pointing at the original grey placeholder are changed, so images set later by an admin are kept.
UPDATE products p
SET image_url = 'https://placehold.co/600x400/' ||
        CASE c.name
            WHEN 'Electronics' THEN '3730a3/ffffff'
            WHEN 'Fashion' THEN 'be185d/ffffff'
            ELSE '0f766e/ffffff'
        END ||
        '?text=' || split_part(p.image_url, '?text=', 2)
FROM categories c
WHERE c.id = p.category_id
  AND p.image_url LIKE 'https://placehold.co/600x400?text=%';
