-- Crea las dos bases de datos independientes, una por microservicio.
-- notification_db no tiene ninguna FK fisica hacia ticket_db: cada
-- servicio es dueno exclusivo de su propia base de datos.
CREATE DATABASE ticket_db;
CREATE DATABASE notification_db;
