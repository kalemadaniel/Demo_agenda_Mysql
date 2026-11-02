-- Structure uniquement : ne restaure pas les données perdues.
CREATE DATABASE IF NOT EXISTS java_calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE java_calendar;
CREATE TABLE IF NOT EXISTS appointment (
    id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    date DATE NOT NULL,
    startTime TIME NOT NULL,
    endTime TIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_appointment_date_start (date, startTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE OR REPLACE VIEW v_calendrier AS
SELECT id, title, description, location, date, startTime, endTime FROM appointment;
