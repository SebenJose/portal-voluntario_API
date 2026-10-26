-- O hash da V900 não correspondia à senha documentada "password".
-- Mantém V900 intacta para não invalidar o checksum de bancos existentes.
UPDATE usuarios
SET senha = '$2a$10$w/TcgU5Yy0FuuAbt74vSuunGy9imfL6mP16etoFko4sCv3dEVIuKq'
WHERE senha = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
