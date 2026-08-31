-- Repeatable seed: master product catalog.
-- Only runs where `spring.flyway.locations` includes `classpath:seed` (local/dev profile).
-- Idempotent: `ON CONFLICT DO NOTHING` matches the ux_master_product_name_lower expression
-- index, so re-runs (and edits to this file) only add the missing rows.
-- Add products by appending rows here and restarting.
INSERT INTO master_product (name, unit) VALUES
    ('Papa',            'KILOGRAMO'),
    ('Tomate',          'KILOGRAMO'),
    ('Cebolla',         'KILOGRAMO'),
    ('Zanahoria',       'KILOGRAMO'),
    ('Zapallo',         'KILOGRAMO'),
    ('Poroto verde',    'KILOGRAMO'),
    ('Ají verde',       'KILOGRAMO'),
    ('Lechuga',         'UNIDAD'),
    ('Choclo',          'UNIDAD'),
    ('Repollo',         'UNIDAD'),
    ('Cilantro',        'ATADO'),
    ('Perejil',         'ATADO'),
    ('Acelga',          'ATADO'),
    ('Espinaca',        'ATADO'),
    ('Manzana',         'KILOGRAMO'),
    ('Palta',           'KILOGRAMO'),
    ('Limón',           'KILOGRAMO'),
    ('Naranja',         'KILOGRAMO'),
    ('Uva',             'KILOGRAMO'),
    ('Frutilla',        'BANDEJA'),
    ('Huevos de campo', 'DOCENA')
ON CONFLICT DO NOTHING;
