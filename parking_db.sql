-- Create the database
CREATE DATABASE IF NOT EXISTS parking_db;
USE parking_db;
-- Create parking_spots table
CREATE TABLE IF NOT EXISTS parking_spots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    spot_number VARCHAR(10) NOT NULL UNIQUE,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    spot_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_spot_type CHECK (spot_type IN ('CAR', 'BIKE', 'TRUCK'))
);

-- Create vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('CAR', 'BIKE', 'TRUCK'))
);

-- Create parking_transactions table
CREATE TABLE IF NOT EXISTS parking_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME,
    fee DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (spot_id) REFERENCES parking_spots(id)
);

-- Create users table for system access
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'OPERATOR', 'USER'))
);

-- Create parking_rates table
CREATE TABLE IF NOT EXISTS parking_rates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_type VARCHAR(20) NOT NULL,
    rate_per_hour DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_rate_vehicle_type CHECK (vehicle_type IN ('CAR', 'BIKE', 'TRUCK'))
);

-- Insert sample parking spots
INSERT INTO parking_spots (spot_number, spot_type, is_occupied) VALUES
('A1', 'CAR', FALSE),
('A2', 'CAR', FALSE),
('A3', 'CAR', FALSE),
('B1', 'BIKE', FALSE),
('B2', 'BIKE', FALSE),
('C1', 'TRUCK', FALSE);

-- Insert sample parking rates
INSERT INTO parking_rates (vehicle_type, rate_per_hour) VALUES
('CAR', 2.00),
('BIKE', 1.00),
('TRUCK', 4.00);

-- Insert admin user (password: admin123)
INSERT INTO users (username, password_hash, role) VALUES
('admin', 'HASHED_PASSWORD_HERE', 'ADMIN');

-- Create view for available parking spots
CREATE VIEW vw_available_spots AS
SELECT 
    spot_type,
    COUNT(*) as available_count
FROM parking_spots
WHERE is_occupied = FALSE
GROUP BY spot_type;

-- Create view for active parking sessions
CREATE VIEW vw_active_sessions AS
SELECT 
    pt.id as transaction_id,
    v.license_plate,
    ps.spot_number,
    pt.entry_time,
    TIMESTAMPDIFF(HOUR, pt.entry_time, CURRENT_TIMESTAMP) as hours_parked,
    (TIMESTAMPDIFF(HOUR, pt.entry_time, CURRENT_TIMESTAMP) * pr.rate_per_hour) as current_fee
FROM parking_transactions pt
JOIN vehicles v ON pt.vehicle_id = v.id
JOIN parking_spots ps ON pt.spot_id = ps.id
JOIN parking_rates pr ON v.vehicle_type = pr.vehicle_type
WHERE pt.exit_time IS NULL;

-- Create stored procedure to park vehicle
DELIMITER //
CREATE PROCEDURE sp_park_vehicle(
    IN p_license_plate VARCHAR(20),
    IN p_vehicle_type VARCHAR(20),
    OUT p_transaction_id BIGINT
)
BEGIN
    DECLARE v_vehicle_id BIGINT;
    DECLARE v_spot_id BIGINT;
    
    START TRANSACTION;
    
    -- Get or create vehicle
    SELECT id INTO v_vehicle_id FROM vehicles WHERE license_plate = p_license_plate;
    IF v_vehicle_id IS NULL THEN
        INSERT INTO vehicles (license_plate, vehicle_type)
        VALUES (p_license_plate, p_vehicle_type);
        SET v_vehicle_id = LAST_INSERT_ID();
    END IF;
    
    -- Find available spot
    SELECT id INTO v_spot_id 
    FROM parking_spots 
    WHERE spot_type = p_vehicle_type 
    AND is_occupied = FALSE 
    LIMIT 1;
    
    IF v_spot_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No parking spot available';
    END IF;
    
    -- Update spot status
    UPDATE parking_spots 
    SET is_occupied = TRUE 
    WHERE id = v_spot_id;
    
    -- Create parking transaction
    INSERT INTO parking_transactions (vehicle_id, spot_id, entry_time)
    VALUES (v_vehicle_id, v_spot_id, CURRENT_TIMESTAMP);
    
    SET p_transaction_id = LAST_INSERT_ID();
    
    COMMIT;
END //
DELIMITER ;

-- Create stored procedure to exit parking
DELIMITER //
CREATE PROCEDURE sp_exit_parking(
    IN p_transaction_id BIGINT,
    OUT p_fee DECIMAL(10, 2)
)
BEGIN
    DECLARE v_entry_time DATETIME;
    DECLARE v_vehicle_type VARCHAR(20);
    DECLARE v_rate_per_hour DECIMAL(10, 2);
    DECLARE v_spot_id BIGINT;
    
    START TRANSACTION;
    
    -- Get transaction details
    SELECT 
        pt.entry_time,
        v.vehicle_type,
        pr.rate_per_hour,
        pt.spot_id
    INTO 
        v_entry_time,
        v_vehicle_type,
        v_rate_per_hour,
        v_spot_id
    FROM parking_transactions pt
    JOIN vehicles v ON pt.vehicle_id = v.id
    JOIN parking_rates pr ON v.vehicle_type = pr.vehicle_type
    WHERE pt.id = p_transaction_id;
    
    -- Calculate fee
    SET p_fee = TIMESTAMPDIFF(HOUR, v_entry_time, CURRENT_TIMESTAMP) * v_rate_per_hour;
    
    -- Update transaction
    UPDATE parking_transactions
    SET exit_time = CURRENT_TIMESTAMP,
        fee = p_fee
    WHERE id = p_transaction_id;
    
    -- Free up parking spot
    UPDATE parking_spots
    SET is_occupied = FALSE
    WHERE id = v_spot_id;
    
    COMMIT;
END //
DELIMITER ;

-- Create trigger to log parking transactions
CREATE TABLE parking_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_type VARCHAR(20),
    transaction_id BIGINT,
    vehicle_id BIGINT,
    spot_id BIGINT,
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details JSON
);

DELIMITER //
CREATE TRIGGER trg_after_parking_insert
AFTER INSERT ON parking_transactions
FOR EACH ROW
BEGIN
    INSERT INTO parking_logs (action_type, transaction_id, vehicle_id, spot_id, details)
    VALUES ('ENTRY', NEW.id, NEW.vehicle_id, NEW.spot_id,
        JSON_OBJECT(
            'entry_time', NEW.entry_time,
            'spot_id', NEW.spot_id
        )
    );
END //

CREATE TRIGGER trg_after_parking_update
AFTER UPDATE ON parking_transactions
FOR EACH ROW
BEGIN
    IF NEW.exit_time IS NOT NULL AND OLD.exit_time IS NULL THEN
        INSERT INTO parking_logs (action_type, transaction_id, vehicle_id, spot_id, details)
        VALUES ('EXIT', NEW.id, NEW.vehicle_id, NEW.spot_id,
            JSON_OBJECT(
                'entry_time', NEW.entry_time,
                'exit_time', NEW.exit_time,
                'fee', NEW.fee
            )
        );
    END IF;
END //
DELIMITER ;