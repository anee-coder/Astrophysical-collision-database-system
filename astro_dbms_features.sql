USE astro_collision;

-- ✅ 1. VIEW: Show only high-energy collisions
CREATE OR REPLACE VIEW HighEnergyCollisions AS
SELECT Collision_ID, Type, Energy, Velocity, Date, Location_ID, Description
FROM Collision
WHERE Energy > 900000;

-- ✅ 2. TRIGGER: Log newly inserted collisions automatically
CREATE TABLE IF NOT EXISTS Collision_Log (
    Log_ID INT AUTO_INCREMENT PRIMARY KEY,
    Collision_ID VARCHAR(10),
    Action VARCHAR(50),
    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //
CREATE TRIGGER after_collision_insert
AFTER INSERT ON Collision
FOR EACH ROW
BEGIN
    INSERT INTO Collision_Log (Collision_ID, Action)
    VALUES (NEW.Collision_ID, 'New Collision Added');
END //
DELIMITER ;

-- ✅ 3. STORED PROCEDURE: Show collisions above a threshold
DELIMITER //
CREATE PROCEDURE ShowHighEnergy()
BEGIN
    SELECT Collision_ID, Type, Energy
    FROM Collision
    WHERE Energy > 1000000;
END //
DELIMITER ;

-- ✅ 4. CURSOR EXAMPLE: Calculate average energy using cursor
DELIMITER //
CREATE PROCEDURE CalculateAverageEnergy()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE e DOUBLE;
    DECLARE total DOUBLE DEFAULT 0;
    DECLARE count INT DEFAULT 0;
    DECLARE cur CURSOR FOR SELECT Energy FROM Collision;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO e;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET total = total + e;
        SET count = count + 1;
    END LOOP;
    CLOSE cur;

    SELECT total / count AS Average_Energy;
END //
DELIMITER ;

-- ✅ 5. SAMPLE SET OPERATION
SELECT Collision_ID, Type FROM Collision
WHERE Type = 'Star-Star'
UNION
SELECT Collision_ID, Type FROM Collision
WHERE Type = 'Galaxy-Galaxy';

-- ✅ 6. SAMPLE SUBQUERY
SELECT Collision_ID, Type, Energy
FROM Collision
WHERE Energy > (SELECT AVG(Energy) FROM Collision);
