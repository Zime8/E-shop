
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS details_order;
DROP TABLE IF EXISTS orders_client;
DROP TABLE IF EXISTS product_availability;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS shops;

SET FOREIGN_KEY_CHECKS = 1;

-- ====================
--  TABELLE DI BASE
-- ====================

CREATE TABLE shops (
                       id_shop      INT PRIMARY KEY,
                       name_s       VARCHAR(100) NOT NULL,
                       street       VARCHAR(150) NOT NULL,
                       phone_number VARCHAR(20)  NOT NULL,
                       balance      DECIMAL(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB;

CREATE TABLE products (
                          product_id INT PRIMARY KEY,
                          name_p     VARCHAR(100) NOT NULL,
                          sport      ENUM('calcio','basket','running','tennis','nuoto') NOT NULL,
                          brand      ENUM('adidas','nike','puma','joma','jordan')       NOT NULL,
                          category   ENUM('abbigliamento','calzature','accessori')      NOT NULL
) ENGINE=InnoDB;

CREATE TABLE product_availability (
                                      id_shop      INT         NOT NULL,
                                      product_id   INT         NOT NULL,
                                      price        DECIMAL(10,2) NOT NULL,
                                      quantity     INT         NOT NULL,
                                      size         VARCHAR(10) NOT NULL,
                                      avg_rating   DECIMAL(3,2) DEFAULT NULL,
                                      review_count INT          NOT NULL DEFAULT 0,
                                      PRIMARY KEY (id_shop, product_id, size),
                                      CONSTRAINT fk_pa_shop FOREIGN KEY (id_shop)    REFERENCES shops(id_shop)      ON DELETE CASCADE,
                                      CONSTRAINT fk_pa_prod FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE users (
                       id_user  INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50)  NOT NULL UNIQUE,
                       pass     VARCHAR(100) NOT NULL,
                       rol      ENUM('cliente','venditore') NOT NULL,
                       id_shop  INT NULL,
                       email    VARCHAR(255) NOT NULL UNIQUE,
                       phone    VARCHAR(12)  NOT NULL,
                       CONSTRAINT fk_users_shop FOREIGN KEY (id_shop) REFERENCES shops(id_shop)
) ENGINE=InnoDB;

CREATE TABLE orders_client (
                               id_order          INT AUTO_INCREMENT PRIMARY KEY,
                               id_user           INT        NOT NULL,
                               date_order        TIMESTAMP  NOT NULL,
                               date_order_update TIMESTAMP  NOT NULL,
                               address           VARCHAR(255) NOT NULL,
                               state_order       ENUM('in elaborazione','spedito','consegnato','annullato')
                   NOT NULL DEFAULT 'in elaborazione',
                               CONSTRAINT fk_oc_user FOREIGN KEY (id_user) REFERENCES users(id_user)
) ENGINE=InnoDB;

CREATE TABLE details_order (
                               id_order  INT         NOT NULL,
                               id_product INT        NOT NULL,
                               id_shop   INT         NOT NULL,
                               quantity  INT         NOT NULL,
                               price     DECIMAL(10,2) NOT NULL,
                               size      VARCHAR(10) NOT NULL,
                               PRIMARY KEY (id_order, id_product, id_shop, size),
                               CONSTRAINT fk_do_order FOREIGN KEY (id_order)  REFERENCES orders_client(id_order) ON DELETE CASCADE,
                               CONSTRAINT fk_do_shop  FOREIGN KEY (id_shop)   REFERENCES shops(id_shop),
                               CONSTRAINT fk_do_prod  FOREIGN KEY (id_product) REFERENCES products(product_id)
) ENGINE=InnoDB;

-- ====================
--  TRIGGER
-- ====================

DELIMITER //
CREATE TRIGGER balance_after_insert
    AFTER INSERT ON details_order
    FOR EACH ROW
BEGIN
    UPDATE shops sh
    SET sh.balance = sh.balance + (NEW.quantity * NEW.price)
    WHERE sh.id_shop = NEW.id_shop;
END//
DELIMITER ;

-- ====================
--  STORED PROCEDURES
-- ====================

-- Catalogo negozio (ricerca opzionale)
DELIMITER //
CREATE PROCEDURE sp_seller_list_catalog(IN p_shop_id INT, IN p_search VARCHAR(255))
BEGIN
SELECT
    pa.product_id,
    p.name_p,
    p.sport,
    p.brand,
    p.category,
    pa.size,
    pa.price,
    pa.quantity
FROM product_availability pa
         JOIN products p ON p.product_id = pa.product_id
WHERE pa.id_shop = p_shop_id
  AND (
    p_search IS NULL
        OR p.name_p   LIKE CONCAT('%', p_search, '%')
        OR p.brand    LIKE CONCAT('%', p_search, '%')
        OR p.category LIKE CONCAT('%', p_search, '%')
        OR p.sport    LIKE CONCAT('%', p_search, '%')
    )
ORDER BY p.name_p, pa.size;
END//
DELIMITER ;

-- Upsert riga catalogo: se esiste, somma le quantità; aggiorna il prezzo
DELIMITER //
CREATE PROCEDURE sp_seller_upsert_catalog(
    IN p_shop_id    INT,
    IN p_product_id INT,
    IN p_size       VARCHAR(10),
    IN p_price      DECIMAL(10,2),
    IN p_qty        INT
)
BEGIN
INSERT INTO product_availability (id_shop, product_id, size, price, quantity)
VALUES (p_shop_id, p_product_id, p_size, p_price, p_qty)
    ON DUPLICATE KEY UPDATE
                         price    = VALUES(price),
                         quantity = product_availability.quantity + VALUES(quantity);
END//
DELIMITER ;

-- Place order transazionale con decremento stock e ritorno mappa shop->order
DELIMITER //
CREATE PROCEDURE sp_place_order(
    IN p_user_id INT,
    IN p_address VARCHAR(255),
    IN p_items   JSON
)
BEGIN
  DECLARE done INT DEFAULT 0;

  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_need (
    product_id BIGINT      NOT NULL,
    shop_id    INT         NOT NULL,
    size       VARCHAR(10) NOT NULL,
    qty_needed INT         NOT NULL,
    PRIMARY KEY (product_id, shop_id, size)
  ) ENGINE=MEMORY;

TRUNCATE tmp_need;

INSERT INTO tmp_need(product_id, shop_id, size, qty_needed)
SELECT jt.product_id, jt.shop_id, jt.size, SUM(jt.quantity)
FROM JSON_TABLE(
             p_items, '$[*]'
            COLUMNS (
           product_id BIGINT      PATH '$.productId',
           shop_id    INT         PATH '$.shopId',   -- fix: shopId minuscolo
           size       VARCHAR(20) PATH '$.size',
           quantity   INT         PATH '$.quantity'
         )
     ) jt
GROUP BY jt.product_id, jt.shop_id, jt.size;

START TRANSACTION;

-- lock per stock check
SELECT 1
FROM product_availability pa
         JOIN tmp_need n
              ON n.product_id = pa.product_id
                  AND n.shop_id    = pa.id_shop
                  AND n.size       = pa.size
    FOR UPDATE;

IF EXISTS (
      SELECT 1
      FROM tmp_need n
      LEFT JOIN product_availability pa
        ON pa.product_id = n.product_id
       AND pa.id_shop    = n.shop_id
       AND pa.size       = n.size
      WHERE pa.quantity IS NULL OR pa.quantity < n.qty_needed
    ) THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insufficiente per uno o più articoli';
END IF;

    CREATE TEMPORARY TABLE IF NOT EXISTS tmp_orders_map (
      shop_id  INT PRIMARY KEY,
      order_id INT
    ) ENGINE=MEMORY;

TRUNCATE tmp_orders_map;

INSERT INTO tmp_orders_map(shop_id, order_id)
SELECT DISTINCT jt.shop_id, NULL
FROM JSON_TABLE(
             p_items, '$[*]'
            COLUMNS (shop_id INT PATH '$.shopId')
     ) jt;

-- crea un ordine per ogni shop
BEGIN
      DECLARE v_shop_id INT;
      DECLARE cur CURSOR FOR SELECT shop_id FROM tmp_orders_map;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

OPEN cur;
read_loop: LOOP
        FETCH cur INTO v_shop_id;
        IF done = 1 THEN LEAVE read_loop; END IF;

INSERT INTO orders_client (id_user, date_order, date_order_update, address, state_order)
VALUES (p_user_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULLIF(TRIM(p_address), ''), 'in elaborazione');

UPDATE tmp_orders_map
SET order_id = LAST_INSERT_ID()
WHERE shop_id = v_shop_id;
END LOOP;
CLOSE cur;
END;

    -- righe dettaglio
INSERT INTO details_order (id_order, id_product, id_shop, size, quantity, price)
SELECT m.order_id, jt.product_id, jt.shop_id, jt.size, jt.quantity, jt.unit_price
FROM JSON_TABLE(
             p_items, '$[*]'
            COLUMNS (
             product_id BIGINT      PATH '$.productId',
             shop_id    INT         PATH '$.shopId',
             size       VARCHAR(20) PATH '$.size',
             quantity   INT         PATH '$.quantity',
             unit_price DECIMAL(10,2) PATH '$.unitPrice'
           )
     ) jt
         JOIN tmp_orders_map m ON m.shop_id = jt.shop_id;

-- decremento stock
UPDATE product_availability pa
    JOIN tmp_need n
ON pa.product_id = n.product_id
    AND pa.id_shop    = n.shop_id
    AND pa.size       = n.size
    SET pa.quantity = pa.quantity - n.qty_needed;

-- result set atteso da OrderDAO (id_shop, id_order)
SELECT shop_id AS id_shop, order_id AS id_order
FROM tmp_orders_map
ORDER BY shop_id;

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_need;
  DROP TEMPORARY TABLE IF EXISTS tmp_orders_map;
END//
DELIMITER ;

-- ====================
--  SEED MINIMO
-- ====================

INSERT INTO shops (id_shop, name_s, street, phone_number, balance) VALUES
    (1, 'Cisalfa Sport', 'Via Torre di Mezzavia 91', '3346723506', 0.00);

-- product_id espliciti per i test (es. test 1 usa productId=3)
INSERT INTO products (product_id, name_p, sport, brand, category) VALUES
    (1, 'Nike Dri-FIT Tee', 'running', 'nike', 'abbigliamento'),                                                                  (2, 'Adidas Barricade', 'tennis',  'adidas', 'calzature'),
    (3, 'Puma Future Z 1.4', 'calcio',  'puma',   'calzature');

-- disponibilità: almeno una riga con quantity >= 5
INSERT INTO product_availability (id_shop, product_id, price, quantity, size) VALUES
    (1, 3, 129.99, 10, '42'),
    (1, 1,  29.99,  8, 'M');

-- utente venditore id=1, cliente id=2 (ordine degli insert controlla gli id)
INSERT INTO users (username, pass, rol, id_shop, email, phone) VALUES
    ('Cisalfa', 'dummy-hash', 'venditore', 1,   'venditore@shop.it', '3330000000'),
    ('Cliente', 'dummy-hash', 'cliente',   NULL,'cliente@test.it',   '3331111111');
