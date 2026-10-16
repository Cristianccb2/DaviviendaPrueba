-- Datos semilla. La BD es nueva en cada arranque, por lo que los IDs generados son deterministas.

-- Polizas
INSERT INTO poliza (tipo, estado, canon_mensual, prima) VALUES ('INDIVIDUAL', 'ACTIVA', 1000.00, 12000.00);   -- id 1
INSERT INTO poliza (tipo, estado, canon_mensual, prima) VALUES ('COLECTIVA', 'ACTIVA', 2500.00, 30000.00);    -- id 2
INSERT INTO poliza (tipo, estado, canon_mensual, prima) VALUES ('COLECTIVA', 'CANCELADA', 1800.00, 21600.00); -- id 3
INSERT INTO poliza (tipo, estado, canon_mensual, prima) VALUES ('INDIVIDUAL', 'RENOVADA', 1200.00, 14400.00); -- id 4

-- Riesgos (las individuales tienen exactamente uno)
INSERT INTO riesgo (poliza_id, descripcion, estado) VALUES (1, 'Casa Calle 10 # 20-30', 'ACTIVO');  -- id 1
INSERT INTO riesgo (poliza_id, descripcion, estado) VALUES (2, 'Apartamento 101', 'ACTIVO');        -- id 2
INSERT INTO riesgo (poliza_id, descripcion, estado) VALUES (2, 'Apartamento 102', 'ACTIVO');        -- id 3
INSERT INTO riesgo (poliza_id, descripcion, estado) VALUES (3, 'Local comercial 5', 'CANCELADO');   -- id 4
INSERT INTO riesgo (poliza_id, descripcion, estado) VALUES (4, 'Oficina 802', 'ACTIVO');            -- id 5
