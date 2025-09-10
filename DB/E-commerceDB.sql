SET sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS e_commerce_db;
CREATE DATABASE e_commerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE e_commerce_db;

SET PERSIST log_bin_trust_function_creators = 1;

CREATE TABLE IF NOT EXISTS shops (
	id_shop INT PRIMARY KEY, 
    name_s VARCHAR(100) NOT NULL,         -- Nome del negozio 
    street VARCHAR(150) NOT NULL,         -- Attributo per la via
    phone_number VARCHAR(20) NOT NULL,     -- Attributo per il numero telefonico
    balance DECIMAL(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB;

LOCK TABLES shops WRITE; 
INSERT INTO shops(id_shop, name_s, street, phone_number) VALUES 
('1', 'Cisalfa Sport', 'Via Torre di Mezzavia 91', '3346723506'),
('2', 'Decathlon', 'Via Appia Nuova 450', '39 02 1234567'),
('3', 'Sport Incontro', 'Via dei Castani 35', '39 06 9876543'),
('4', 'Under Armour', 'Via del Tritone 176', '39 081 2233445'),
('5', 'JD Sports', 'Via Luigi Schiavonetti 426', '39 055 3344556');
UNLOCK TABLES; 

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,       -- Id del prodotto
	name_p VARCHAR(100) NOT NULL,                    -- Nome del prodotto
    sport ENUM('calcio', 'basket', 'running', 'tennis', 'nuoto') NOT NULL, -- Sport
    brand ENUM('adidas', 'nike', 'puma', 'joma', 'jordan') NOT NULL,       -- Marca
    category ENUM('abbigliamento', 'calzature', 'accessori') NOT NULL      -- Tipologia 
) ENGINE=InnoDB; 

LOCK TABLES products WRITE; 
INSERT INTO products (name_p, sport, brand, category) VALUES
('Nike Mercurial Vapor 15', 'calcio', 'nike', 'calzature'),('Adidas Predator Accuracy', 'calcio', 'adidas', 'calzature'),
('Puma Future Z 1.4', 'calcio', 'puma', 'calzature'),('Joma Aguila Gol', 'calcio', 'joma', 'calzature'),
('Nike Dri-FIT Strike Jersey', 'calcio', 'nike', 'abbigliamento'),('Adidas Tiro 23 Training Pants', 'calcio', 'adidas', 'abbigliamento'),
('Puma TeamLIGA Shorts', 'calcio', 'puma', 'abbigliamento'),('Joma Champion IV Jersey', 'calcio', 'joma', 'abbigliamento'),
('Nike Guard Lock Shin Guards', 'calcio', 'nike', 'accessori'),('Adidas X League Shin Guards', 'calcio', 'adidas', 'accessori'),
('Jordan Zion 3', 'basket', 'jordan', 'calzature'),('Nike LeBron Witness 8', 'basket', 'nike', 'calzature'),
('Adidas Harden Vol. 7', 'basket', 'adidas', 'calzature'),('Puma MB.02', 'basket', 'puma', 'calzature'),
('Jordan Dri-FIT Sport Hoodie', 'basket', 'jordan', 'abbigliamento'),('Nike Standard Issue Shorts', 'basket', 'nike', 'abbigliamento'),
('Adidas Basketball Tank Top', 'basket', 'adidas', 'abbigliamento'),('Puma Rebound Jersey', 'basket', 'puma', 'abbigliamento'),
('Jordan Elite Wristbands', 'basket', 'jordan', 'accessori'),('Nike Elite Headband', 'basket', 'nike', 'accessori'),
('Nike Air Zoom Pegasus 40', 'running', 'nike', 'calzature'),('Adidas Ultraboost Light', 'running', 'adidas', 'calzature'),
('Puma Deviate Nitro 2', 'running', 'puma', 'calzature'),('Joma R-5000', 'running', 'joma', 'calzature'),
('Nike Dri-FIT Miler Shirt', 'running', 'nike', 'abbigliamento'),('Adidas Own The Run Jacket', 'running', 'adidas', 'abbigliamento'),
('Puma Run Favourite Tee', 'running', 'puma', 'abbigliamento'),('Joma Elite VIII Pants', 'running', 'joma', 'abbigliamento'),
('Nike Lean Arm Band', 'running', 'nike', 'accessori'),('Adidas Running Belt', 'running', 'adidas', 'accessori'),
('Nike Swim Fins', 'nuoto', 'nike', 'accessori'),('Adidas Adizero Swim Briefs', 'nuoto', 'adidas', 'abbigliamento'),
('Puma Swim Shorts', 'nuoto', 'puma', 'abbigliamento'),('Joma Swim Goggles Pro', 'nuoto', 'joma', 'accessori'),
('Nike Hydroguard Shirt', 'nuoto', 'nike', 'abbigliamento'),('Adidas Swim Pro One-Piece', 'nuoto', 'adidas', 'abbigliamento'),
('Puma Silicone Cap', 'nuoto', 'puma', 'accessori'),('Joma Swim Training Suit', 'nuoto', 'joma', 'abbigliamento'),
('Nike Swim Nose Clip', 'nuoto', 'nike', 'accessori'),('Adidas Swim Mesh Bag', 'nuoto', 'adidas', 'accessori'),
('NikeCourt Air Zoom Vapor Pro 2', 'tennis', 'nike', 'calzature'),('Adidas Barricade 13', 'tennis', 'adidas', 'calzature'),
('Puma Court Rider', 'tennis', 'puma', 'calzature'),('Joma Slam Men', 'tennis', 'joma', 'calzature'),
('NikeCourt Dri-FIT Polo', 'tennis', 'nike', 'abbigliamento'),('Adidas Tennis Freelift Tee', 'tennis', 'adidas', 'abbigliamento'),
('Puma Tennis Graphic Shirt', 'tennis', 'puma', 'abbigliamento'),('Joma Tennis Skirt', 'tennis', 'joma', 'abbigliamento'),
('NikeCourt Advantage Wristbands', 'tennis', 'nike', 'accessori'),('Adidas Tennis Headband', 'tennis', 'adidas', 'accessori'),
('Puma One 5.1', 'calcio', 'puma', 'calzature'),('Joma Propulsion Lite', 'calcio', 'joma', 'calzature'),
('Jordan Air 200E', 'basket', 'jordan', 'calzature'),('Nike KD 16', 'basket', 'nike', 'calzature'),
('Adidas D Rose 11', 'basket', 'adidas', 'calzature'),('Nike Tempo Shorts', 'running', 'nike', 'abbigliamento'),
('Joma Suez Hoodie', 'running', 'joma', 'abbigliamento'),('Adidas Swim Cap Pro', 'nuoto', 'adidas', 'accessori'),
('Jordan Courtside Tank', 'basket', 'jordan', 'calzature'),('NikeCourt Visor', 'tennis', 'nike', 'accessori'),
('Puma Ultraride', 'running', 'puma', 'calzature'),('Adidas Solarboost 5', 'running', 'adidas', 'calzature'),
('Nike Victory G Lite', 'tennis', 'nike', 'calzature'),('Joma T.Masters 1000', 'tennis', 'joma', 'calzature'),
('Adidas X Speedportal', 'calcio', 'adidas', 'calzature'),('Jordan Jumpman Wristbands', 'basket', 'jordan', 'accessori'),
('Nike Pro Compression Top', 'basket', 'nike', 'abbigliamento'),('Puma Ultra Play', 'calcio', 'puma', 'calzature'),
('Joma Classic Socks', 'calcio', 'joma', 'accessori'),('Nike Run Division Jacket', 'running', 'nike', 'abbigliamento'),
('Adidas Adizero Prime X', 'running', 'adidas', 'calzature'),('Joma R.Sprint', 'running', 'joma', 'calzature'),
('Nike Swim Essential Briefs', 'nuoto', 'nike', 'abbigliamento'),('Adidas Swim Slides', 'nuoto', 'adidas', 'accessori'),
('Jordan Hydro Slide', 'nuoto', 'jordan', 'accessori'),('NikeCourt Jacket', 'tennis', 'nike', 'abbigliamento'),
('Adidas Tennis Towel', 'tennis', 'adidas', 'accessori'),('Joma Tennis Bag Pro', 'tennis', 'joma', 'accessori'),
('Puma Court Jacket', 'tennis', 'puma', 'abbigliamento'),('NikeCourt Zoom NXT', 'tennis', 'nike', 'calzature'),
('Adidas Match Skirt', 'tennis', 'adidas', 'abbigliamento'),('Puma Grip Bag', 'tennis', 'puma', 'accessori'),
('Joma Sport Socks 3-Pack', 'running', 'joma', 'accessori'),('Nike Run Belt', 'running', 'nike', 'accessori'),
('Adidas Calcio Gloves', 'calcio', 'adidas', 'accessori'),('Jordan Shooting Sleeve', 'basket', 'jordan', 'accessori'),
('Puma Swim Backpack', 'nuoto', 'puma', 'accessori'),('Joma Performance Tank', 'nuoto', 'joma', 'abbigliamento');
UNLOCK TABLES; 

CREATE TABLE IF NOT EXISTS product_availability (
    id_shop INT NOT NULL,
    product_id INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    size VARCHAR(10) NOT NULL,
    avg_rating DECIMAL(3,2) DEFAULT NULL,
    review_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_shop, product_id, size),
    FOREIGN KEY (id_shop) REFERENCES shops(id_shop) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

LOCK TABLES product_availability WRITE;
INSERT INTO product_availability(id_shop, product_id, price, quantity, size) VALUES
('1', 3, 129.99, 25, '42'),('1', 3, 129.99, 40, '40'),('1', 3, 129.99, 30, '36'),('1', 7, 59.90, 40, 'M'),('1', 15, 28.50, 12, 'L'),('1', 21, 110.00, 30, '44'),
('1', 25, 38.75, 20, 'S'),('1', 29, 12.99, 35, 'unique'),('1', 34, 85.00, 22, 'unique'),('1', 38, 15.50, 18, 'S'),
('1', 42, 150.00, 15, '43'),('1', 47, 68.99, 28, 'M'),('1', 51, 40.00, 33, '45'),('1', 56, 55.50, 21, 'S'),
('1', 59, 99.99, 17, '45'),('1', 63, 32.00, 26, '42'),('1', 68, 14.99, 40, '44'),('1', 72, 120.00, 10, '44'),
('1', 75, 80.00, 15, '42'),('1', 79, 25.99, 30, 'S'),('1', 83, 140.00, 14, 'unique'),('1', 87, 65.00, 23, 'unique'),
('1', 90, 22.50, 40, 'unique'),('1', 95, 115.00, 19, '43'),('1', 98, 49.99, 27, 'S'),('1', 100, 17.99, 36, 'unique'),
('1', 5, 130.00, 24, 'M'),('1', 10, 60.00, 22, 'unique'),('1', 14, 30.00, 18, '40'),('1', 19, 18.50, 28, 'unique'),
('1', 22, 75.00, 15, '43'),('1', 27, 39.99, 34, 'S'),('1', 31, 12.00, 40, 'unique'),('1', 36, 95.00, 12, 'M'),
('1', 40, 85.00, 20, 'unique'),('1', 44, 25.00, 22, '43'),('1', 49, 110.00, 17, 'unique'),('1', 53, 60.00, 30, '42'),
('1', 57, 19.99, 38, 'XL'),('1', 61, 125.00, 13, '44'),('1', 66, 50.00, 29, 'unique'),('1', 70, 15.00, 40, 'L'),
('2', 2, 135.00, 18, '43'),('2', 6, 55.00, 22, 'M'),('2', 13, 32.99, 30, '40'),('2', 18, 120.00, 15, 'S'),
('2', 23, 28.50, 25, '42'),('2', 26, 14.99, 40, 'L'),('2', 33, 75.00, 20, 'XL'),('2', 37, 20.00, 18, 'unique'),
('2', 41, 155.00, 10, '42'),('2', 46, 70.00, 27, 'M'),('2', 50, 35.00, 32, 'unique'),('2', 55, 60.00, 21, '43'),
('2', 58, 105.00, 16, 'unique'),('2', 62, 34.99, 29, '43'),('2', 67, 16.99, 38, 'L'),('2', 71, 115.00, 12, '44'),
('2', 74, 82.00, 17, '43'),('2', 78, 26.50, 30, 'unique'),('2', 82, 145.00, 14, 'unique'),('2', 86, 67.99, 22, 'unique'),
('2', 89, 23.00, 35, 'unique'),('2', 93, 110.00, 19, '42'),('2', 97, 50.00, 27, 'S'),('2', 99, 18.50, 36, 'unique'),
('2', 4, 125.00, 24, '41'),('2', 9, 62.00, 20, 'unique'),('2', 12, 29.00, 18, '41'),('2', 17, 19.99, 28, 'M'),
('2', 20, 78.00, 15, 'unique'),('2', 24, 42.00, 33, '43'),('2', 28, 13.50, 40, 'M'),('2', 32, 90.00, 14, 'M'),
('2', 35, 87.00, 21, 'M'),('2', 39, 27.00, 23, 'unique'),('2', 43, 108.00, 15, '42'),('2', 48, 55.00, 30, 'L'),
('2', 52, 22.99, 38, '40'),('2', 60, 128.00, 13, 'unique'),('2', 65, 48.00, 29, '41'),('2', 69, 14.00, 40, 'unique'),
('3', 3, 140.00, 20, '42'),('3', 7, 65.00, 25, 'M'),('3', 10, 30.00, 35, 'unique'),('3', 14, 110.00, 18, '44'),
('3', 19, 25.00, 27, 'unique'),('3', 21, 15.99, 40, '44'),('3', 29, 78.00, 22, 'unique'),('3', 34, 22.00, 19, 'unique'),
('3', 38, 160.00, 12, 'XL'),('3', 42, 73.00, 28, '41'),('3', 47, 36.00, 31, 'L'),('3', 53, 58.00, 23, '42'),
('3', 56, 102.00, 16, 'L'),('3', 61, 30.99, 29, '44'),('3', 66, 18.99, 39, 'unique'),('3', 70, 117.00, 15, 'M'),
('3', 75, 85.00, 17, '43'),('3', 79, 24.50, 33, 'M'),('3', 83, 140.00, 14, 'unique'),('3', 87, 66.99, 21, 'unique'),
('3', 90, 22.00, 34, 'unique'),('3', 94, 105.00, 18, '42'),('3', 98, 52.00, 27, 'S'),('3', 100, 19.50, 35, 'unique'),
('3', 5, 130.00, 26, 'L'),('3', 8, 60.00, 21, 'M'),('3', 11, 28.00, 19, '42'),('3', 15, 20.99, 29, 'M'),
('3', 22, 77.00, 17, '43'),('3', 25, 40.00, 32, 'S'),('3', 27, 14.00, 38, 'XL'),('3', 30, 92.00, 15, 'unique'),
('3', 31, 88.00, 22, 'unique'),('3', 36, 28.00, 20, 'L'),('3', 40, 110.00, 18, 'unique'),('3', 44, 54.00, 31, '40'),
('3', 49, 23.50, 37, 'unique'),('3', 51, 125.00, 14, '44'),('3', 54, 45.00, 28, '40'),('3', 57, 15.50, 40, 'M'),
('4', 4, 145.00, 19, '43'),('4', 6, 70.00, 26, 'M'),('4', 9, 33.00, 24, 'unique'),('4', 13, 115.00, 21, '44'),
('4', 18, 27.50, 30, 'S'),('4', 20, 16.50, 39, 'unique'),('4', 28, 80.00, 22, 'XL'),('4', 33, 25.00, 18, 'L'),
('4', 37, 155.00, 14, 'unique'),('4', 41, 75.00, 29, '44'),('4', 46, 38.00, 33, 'L'),('4', 52, 60.00, 27, '43'),
('4', 55, 100.00, 16, '45'),('4', 60, 31.00, 25, 'unique'),('4', 65, 20.00, 37, '44'),('4', 71, 120.00, 15, '44'),
('4', 74, 88.00, 20, '41'),('4', 78, 26.00, 34, 'unique'),('4', 82, 135.00, 13, 'unique'),('4', 86, 70.00, 23, 'unique'),
('4', 91, 23.00, 36, 'unique'),('4', 93, 110.00, 18, '42'),('4', 97, 55.00, 28, 'S'),('4', 99, 21.00, 31, 'unique'),
('4', 2, 125.00, 27, '41'),('4', 5, 62.00, 22, 'M'),('4', 12, 29.00, 20, '41'),('4', 16, 22.00, 30, 'S'),
('4', 23, 79.00, 18, '43'),('4', 26, 42.00, 35, 'S'),('4', 32, 15.00, 40, 'S'),('4', 35, 95.00, 17, 'S'),
('4', 39, 90.00, 21, 'unique'),('4', 43, 27.00, 19, 'unique'),('4', 45, 105.00, 16, 'L'),('4', 48, 57.00, 29, 'L'),
('4', 50, 24.00, 33, 'unique'),('4', 58, 120.00, 15, 'unique'),('4', 59, 46.00, 25, '40'),('4', 62, 14.50, 38, '40'),
('5', 3, 140.00, 22, '42'),('5', 7, 68.00, 27, 'M'),('5', 11, 34.00, 20, '41'),('5', 14, 112.00, 19, '44'),
('5', 17, 28.00, 30, 'S'),('5', 21, 15.00, 35, '41'),('5', 25, 79.00, 18, 'XL'),('5', 29, 24.00, 21, 'unique'),
('5', 34, 150.00, 14, 'unique'),('5', 38, 76.00, 26, 'M'),('5', 42, 37.00, 32, '43'),('5', 47, 58.00, 28, 'M'),
('5', 53, 99.00, 15, '45'),('5', 56, 33.00, 24, 'S'),('5', 63, 19.00, 37, '43'),('5', 67, 122.00, 16, 'S'),
('5', 70, 90.00, 23, 'M'),('5', 75, 25.00, 31, '40'),('5', 79, 137.00, 13, 'L'),('5', 83, 68.00, 22, 'unique'),
('5', 87, 22.00, 34, 'unique'),('5', 89, 113.00, 18, '43'),('5', 92, 53.00, 28, 'S'),('5', 95, 20.00, 30, 'unique'),
('5', 1, 124.00, 26, '41'),('5', 8, 61.00, 22, 'M'),('5', 10, 27.00, 20, 'unique'),('5', 15, 21.00, 29, 'L'),
('5', 22, 77.00, 18, '43'),('5', 24, 44.00, 35, '40'),('5', 30, 16.00, 40, 'unique'),('5', 36, 93.00, 17, 'XL'),
('5', 40, 91.00, 21, 'unique'),('5', 44, 28.00, 19, '42'),('5', 49, 107.00, 16, 'unique'),('5', 51, 55.00, 29, '41'),
('5', 54, 23.00, 33, '41'),('5', 57, 118.00, 15, 'L'),('5', 61, 45.00, 25, '44'),('5', 64, 13.50, 38, '45');
UNLOCK TABLES; 

CREATE TABLE IF NOT EXISTS users ( 
  id_user INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE, 
  pass VARCHAR(100) NOT NULL, 
  rol ENUM('cliente', 'venditore') NOT NULL,
  id_shop INT NULL,                                          
  email VARCHAR(255) NOT NULL UNIQUE, 
  phone VARCHAR(12) NOT NULL,
  CONSTRAINT fk_users_shop
    FOREIGN KEY (id_shop) REFERENCES shops(id_shop)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT chk_vendor_has_shop
    CHECK (rol <> 'venditore' OR id_shop IS NOT NULL),
  CONSTRAINT chk_client_no_shop
    CHECK (rol <> 'cliente' OR id_shop IS NULL),
  INDEX idx_users_shop (id_shop)
) ENGINE=InnoDB;
 

LOCK TABLES users WRITE; 	
INSERT INTO users(username, pass, rol, id_shop, email, phone) VALUES 
('Cisalfa Sport', '$2a$12$aW/N42Ru603hMB7H/kvYtOcGZqL9PkKs4rbSg0QtPxiPaUKRUYtGK', 'venditore', 1, 'sportguru@gmail.com', '3346723506'),
('Zime8', '$2a$12$8QUT5aJOLaaxY9ZHgO1NOu3McYEtKE4Wz22bsi5Zgb2SBq1DNy6uS', 'cliente', NULL, 'gabriele.simeoni8@gmail.com', '3388415376'),
('Decathlon', '$2a$12$naT9D18jU5/YhrzBpgOcC.5CXed.dJ7OPp5sxeVkIcSLk7HqeQJbu', 'venditore', 2, 'decathlon@gmail.com', '3391122334'),
('Sport Incontro', '$2a$12$0bgGMOmvyy2V5QQesbpjSuEU4QVrQXc7.6JVZKFtFh1CnJ/gPwigG', 'venditore', 3, 'sportincontro@gmail.com', '3335566778'),
('Under Armour', '$2a$12$KHl2Bt4xYdvT3lrOY9wfU.iXWsVHjaLSbwvk4yYpxCdKgwO7NExNO', 'venditore', 4, 'underarmour@gmail.com', '3349988776'),
('JD Sports', '$2a$12$oj/SCU8QVmii/ece1SxZyuGKt4jr/ckDMNOhL9cUxFpv/NPexfHdG', 'venditore', 5, 'jdsports@gmail.com', '3204455667');
UNLOCK TABLES;

-- Tabella per mantenere ordini cliente 
CREATE TABLE IF NOT EXISTS orders_client (
	id_order INT PRIMARY KEY AUTO_INCREMENT, 
    id_user INT NOT NULL,
    date_order TIMESTAMP NOT NULL,
    date_order_update TIMESTAMP NOT NULL,
    address VARCHAR(255) NOT NULL,
    state_order ENUM('in elaborazione', 'spedito', 'consegnato', 'annullato') DEFAULT 'in elaborazione', 
    FOREIGN KEY (id_user) REFERENCES users(id_user)
) ENGINE=InnoDB;

-- Tabella per dettaglio dei singoli ordini
CREATE TABLE IF NOT EXISTS details_order (
	id_order INT NOT NULL, 
    id_product INT NOT NULL, 
    id_shop INT NOT NULL, 
    quantity INT NOT NULL, 
    price DECIMAL(10, 2) NOT NULL, 
    size VARCHAR(10) NOT NULL,
    PRIMARY KEY(id_order, id_product, id_shop, size),
    FOREIGN KEY(id_order) REFERENCES orders_client(id_order), 
    FOREIGN KEY(id_shop) REFERENCES shops(id_shop),
    FOREIGN KEY(id_product) REFERENCES products(product_id)
) ENGINE=InnoDB; 

CREATE TABLE IF NOT EXISTS wishlist (
  username VARCHAR(50) NOT NULL,
  product_id INT NOT NULL,
  id_shop INT NOT NULL,
  p_size VARCHAR(10) NOT NULL,
  PRIMARY KEY (username, product_id, id_shop, p_size),
  FOREIGN KEY (username) REFERENCES users(username),
  FOREIGN KEY (product_id) REFERENCES products(product_id),
  FOREIGN KEY (id_shop) REFERENCES shops(id_shop)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS product_reviews;
CREATE TABLE product_reviews (
  review_id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  id_shop INT NOT NULL,
  id_user INT NOT NULL,
  rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  title VARCHAR(100),
  p_comment TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_prod_shop_user (product_id, id_shop, id_user),
  KEY idx_prod_shop (product_id, id_shop),
  KEY idx_shop (id_shop),
  KEY idx_user (id_user),
  FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
  FOREIGN KEY (id_shop)    REFERENCES shops(id_shop)       ON DELETE CASCADE,
  FOREIGN KEY (id_user)    REFERENCES users(id_user)       ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS saved_cards;
CREATE TABLE saved_cards (
  card_id     INT AUTO_INCREMENT PRIMARY KEY,
  id_user     INT NOT NULL,              
  holder      VARCHAR(100) NOT NULL,     
  card_number VARCHAR(32)  NOT NULL,     
  expiry      VARCHAR(7)   NOT NULL,      
  card_type   VARCHAR(16)  NOT NULL,      
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Triggers per aggiornamenti automatici 
DELIMITER //

CREATE TRIGGER psr_after_insert
AFTER INSERT ON product_reviews
FOR EACH ROW
BEGIN
  UPDATE product_availability pa
  SET pa.avg_rating  = (SELECT AVG(rating) FROM product_reviews WHERE product_id = NEW.product_id AND id_shop = NEW.id_shop),
      pa.review_count = (SELECT COUNT(*)   FROM product_reviews WHERE product_id = NEW.product_id AND id_shop = NEW.id_shop)
  WHERE pa.product_id = NEW.product_id AND pa.id_shop = NEW.id_shop;
END //

CREATE TRIGGER psr_after_update
AFTER UPDATE ON product_reviews
FOR EACH ROW
BEGIN
  UPDATE product_availability pa
  SET pa.avg_rating  = (SELECT AVG(rating) FROM product_reviews WHERE product_id = NEW.product_id AND id_shop = NEW.id_shop),
      pa.review_count = (SELECT COUNT(*)   FROM product_reviews WHERE product_id = NEW.product_id AND id_shop = NEW.id_shop)
  WHERE pa.product_id = NEW.product_id AND pa.id_shop = NEW.id_shop;

  IF (OLD.product_id <> NEW.product_id) OR (OLD.id_shop <> NEW.id_shop) THEN
    UPDATE product_availability pa
    SET pa.avg_rating  = (SELECT AVG(rating) FROM product_reviews WHERE product_id = OLD.product_id AND id_shop = OLD.id_shop),
        pa.review_count = (SELECT COUNT(*)   FROM product_reviews WHERE product_id = OLD.product_id AND id_shop = OLD.id_shop)
    WHERE pa.product_id = OLD.product_id AND pa.id_shop = OLD.id_shop;
  END IF;
END //

CREATE TRIGGER psr_after_delete
AFTER DELETE ON product_reviews
FOR EACH ROW
BEGIN
  UPDATE product_availability pa
  SET pa.avg_rating  = (SELECT AVG(rating) FROM product_reviews WHERE product_id = OLD.product_id AND id_shop = OLD.id_shop),
      pa.review_count = (SELECT COUNT(*)   FROM product_reviews WHERE product_id = OLD.product_id AND id_shop = OLD.id_shop)
  WHERE pa.product_id = OLD.product_id AND pa.id_shop = OLD.id_shop;
END //

DELIMITER //
CREATE TRIGGER balance_after_insert
AFTER INSERT ON DETAILS_ORDER
FOR EACH ROW
BEGIN
  UPDATE SHOPS sh
  SET sh.balance  = sh.balance + ( NEW.quantity * NEW.price )
  WHERE sh.id_shop = NEW.id_shop;
END //

DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;

-- STORED PROCEDURES

-- ProductDAO

-- Ultimi prodotti
DELIMITER //
CREATE PROCEDURE sp_find_latest(IN p_limit INT)
BEGIN
  SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
         pa.price AS price, p.image_data, p.created_at,
         s.name_s AS shop_name, pa.quantity, pa.size, s.id_shop
  FROM products p
  JOIN product_availability pa ON pa.product_id = p.product_id
  JOIN shops s ON s.id_shop = pa.id_shop
  LEFT JOIN product_availability pa2
    ON pa2.product_id = pa.product_id AND pa2.id_shop = pa.id_shop
   AND (pa2.price < pa.price OR (pa2.price = pa.price AND pa2.size < pa.size))
  WHERE pa2.product_id IS NULL AND pa.quantity > 0
  ORDER BY p.created_at DESC, p.product_id DESC, s.id_shop ASC
  LIMIT p_limit;
END//
DELIMITER ;

-- Ricerca per nome
DELIMITER //
CREATE PROCEDURE sp_search_by_name(IN p_name VARCHAR(255))
BEGIN
  SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
         MIN(pa.price) AS price, p.image_data, p.created_at,
         s.name_s AS shop_name, s.id_shop
  FROM products p
  JOIN product_availability pa ON p.product_id = pa.product_id
  JOIN shops s ON pa.id_shop = s.id_shop
  WHERE LOWER(p.name_p) LIKE CONCAT('%', LOWER(IFNULL(p_name,'')), '%')
  GROUP BY p.product_id, p.name_p, p.sport, p.brand, p.category, p.image_data, p.created_at, s.name_s, s.id_shop
  ORDER BY p.created_at DESC;
END//
DELIMITER ;

-- Ricerca con filtri e range di prezzo
DELIMITER //
CREATE PROCEDURE sp_search_by_filters(
  IN p_sport VARCHAR(100),
  IN p_brand VARCHAR(100),
  IN p_shop_id INT,
  IN p_category VARCHAR(100),
  IN p_min DOUBLE,
  IN p_max DOUBLE
)
BEGIN
  SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
         MIN(pa.price) AS price, p.image_data, p.created_at,
         s.name_s AS shop_name, s.id_shop
  FROM products p
  JOIN product_availability pa ON p.product_id = pa.product_id
  JOIN shops s ON pa.id_shop = s.id_shop
  WHERE pa.price BETWEEN p_min AND p_max
    AND (p_sport    IS NULL OR p.sport    = p_sport)
    AND (p_brand    IS NULL OR p.brand    = p_brand)
    AND (p_shop_id  IS NULL OR s.id_shop  = p_shop_id)
    AND (p_category IS NULL OR p.category = p_category)
  GROUP BY p.product_id, p.name_p, p.sport, p.brand, p.category, p.image_data, p.created_at, s.name_s, s.id_shop
  ORDER BY p.created_at DESC;
END//
DELIMITER ;

-- Shop id per nome 
DELIMITER //
CREATE PROCEDURE sp_get_shop_id_by_name(IN p_name_s VARCHAR(255))
BEGIN
  SELECT id_shop FROM shops WHERE name_s = p_name_s LIMIT 1;
END//
DELIMITER ;

-- Taglie disponibili per prodotto/shop
DELIMITER //
CREATE PROCEDURE sp_get_available_sizes(IN p_product_id BIGINT, IN p_shop_id INT)
BEGIN
  SELECT size
  FROM product_availability
  WHERE product_id = p_product_id AND id_shop = p_shop_id AND quantity > 0
  ORDER BY size ASC;
END//
DELIMITER ;

-- Prezzo per prodotto/shop/size
DELIMITER //
CREATE PROCEDURE sp_get_price_for(
  IN p_product_id BIGINT, IN p_shop_id INT, IN p_size VARCHAR(20), OUT p_price DOUBLE
)
BEGIN
  SELECT price INTO p_price
  FROM product_availability
  WHERE product_id = p_product_id AND id_shop = p_shop_id AND size = p_size
  LIMIT 1;
END//
DELIMITER ;

-- Stock per prodotto/shop/size 
DELIMITER //
CREATE PROCEDURE sp_get_stock_for(
  IN p_product_id BIGINT, IN p_shop_id INT, IN p_size VARCHAR(20), OUT p_qty INT
)
BEGIN
  SELECT quantity INTO p_qty
  FROM product_availability
  WHERE product_id = p_product_id AND id_shop = p_shop_id AND size = p_size
  LIMIT 1;
END//
DELIMITER ;

-- Esistenza in wishlist 
DELIMITER //
CREATE PROCEDURE sp_exists_wish(
  IN p_username VARCHAR(255), IN p_product_id BIGINT, IN p_shop_id INT, IN p_size VARCHAR(20), OUT p_exists TINYINT
)
BEGIN
  SELECT COUNT(*) > 0 INTO p_exists
  FROM wishlist
  WHERE username = p_username AND product_id = p_product_id AND id_shop = p_shop_id
    AND (p_size IS NULL OR p_size = wishlist.p_size)
  LIMIT 1;
END//
DELIMITER ;

-- OrderDAO

DELIMITER //

DROP PROCEDURE IF EXISTS sp_place_order //
CREATE PROCEDURE sp_place_order(
  IN p_user_id INT,
  IN p_address VARCHAR(255),
  IN p_items   JSON
)
BEGIN
  DECLARE done INT DEFAULT 0;

  /* Tabella temporanea per (product, shop, size) */
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_need(
    product_id BIGINT NOT NULL,
    shop_id    INT    NOT NULL,
    size       VARCHAR(20) NOT NULL,
    qty_needed INT    NOT NULL,
    PRIMARY KEY (product_id, shop_id, size)
  ) ENGINE=MEMORY;

  TRUNCATE tmp_need;

  /* Espande JSON in righe e aggrega */
  INSERT INTO tmp_need(product_id, shop_id, size, qty_needed)
  SELECT jt.product_id, jt.shop_id, jt.size, SUM(jt.quantity)
  FROM JSON_TABLE(
         p_items, '$[*]'
         COLUMNS (
           product_id BIGINT      PATH '$.productId',
           shop_id    INT         PATH '$.shopId',
           size       VARCHAR(20) PATH '$.size',
           quantity   INT         PATH '$.quantity'
         )
       ) jt
  GROUP BY jt.product_id, jt.shop_id, jt.size;

  /* Verifica disponibilità e DECREMENTA lo stock in modo atomico */
  SET @need_rows := (SELECT COUNT(*) FROM tmp_need);

  UPDATE product_availability pa
  JOIN tmp_need n
    ON pa.product_id = n.product_id
   AND pa.id_shop    = n.shop_id
   AND pa.size       = n.size
  SET pa.quantity = pa.quantity - n.qty_needed
  WHERE pa.quantity >= n.qty_needed;

  /* L'UPDATE deve toccare esattamente @need_rows righe */
  IF ROW_COUNT() <> @need_rows THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insufficiente per uno o più articoli';
  END IF;

  /* Mappa shop -> order_id */
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_orders_map(
    shop_id  INT PRIMARY KEY,
    order_id INT
  ) ENGINE=MEMORY;

  TRUNCATE tmp_orders_map;

  INSERT INTO tmp_orders_map(shop_id, order_id)
  SELECT DISTINCT shop_id, NULL
  FROM tmp_need;

  /* Crea un ordine per ciascuno shop e salva l'order_id */
  BEGIN
    DECLARE v_shop_id INT;
    DECLARE cur CURSOR FOR SELECT shop_id FROM tmp_orders_map;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;
    read_loop: LOOP
      FETCH cur INTO v_shop_id;
      IF done = 1 THEN LEAVE read_loop; END IF;

      INSERT INTO orders_client(id_user, date_order, date_order_update, state_order, address)
      VALUES (p_user_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'in elaborazione', NULLIF(TRIM(p_address), ''));

      UPDATE tmp_orders_map
      SET order_id = LAST_INSERT_ID()
      WHERE shop_id = v_shop_id;
    END LOOP;
    CLOSE cur;
  END;

  /* Inserisce righe d’ordine per ciascuna riga del JSON, mappando per shop */
  INSERT INTO details_order(id_order, id_product, id_shop, quantity, price, size)
  SELECT m.order_id, jt.product_id, jt.shop_id, jt.quantity, jt.unit_price, jt.size
  FROM JSON_TABLE(
       p_items, '$[*]'
       COLUMNS (
         product_id  BIGINT        PATH '$.productId',
         shop_id     INT           PATH '$.shopId',        
         size        VARCHAR(20)   PATH '$.size',
         quantity    INT           PATH '$.quantity',
         unit_price  DECIMAL(10,2) PATH '$.unitPrice'
       )
     ) jt
  JOIN tmp_orders_map m ON m.shop_id = jt.shop_id;


  /* Restituisce il risultato finale */
  SELECT m.shop_id AS id_shop, m.order_id AS id_order
  FROM tmp_orders_map m
  ORDER BY m.shop_id;

  /* Pulizia */
  DROP TEMPORARY TABLE IF EXISTS tmp_need;
  DROP TEMPORARY TABLE IF EXISTS tmp_orders_map;
END //

DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_list_orders_header(IN p_user_id INT)
BEGIN
  SELECT id_order, id_user, date_order, state_order
  FROM orders_client
  WHERE id_user = p_user_id
  ORDER BY date_order DESC, id_order DESC;
END//
DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_list_orders_lines(IN p_user_id INT)
BEGIN
  SELECT d.id_order, d.id_product, d.id_shop, d.size, d.quantity, d.price,
         p.name_p AS product_name, s.name_s AS shop_name
  FROM details_order d
  JOIN orders_client o ON o.id_order = d.id_order
  JOIN products p ON p.product_id = d.id_product
  JOIN shops    s ON s.id_shop    = d.id_shop
  WHERE o.id_user = p_user_id
  ORDER BY d.id_order ASC, p.name_p ASC, d.id_product ASC;
END//
DELIMITER ;

-- ReviewDAO

-- Lista recensioni per prodotto
DELIMITER //
CREATE PROCEDURE sp_list_reviews(
  IN p_product_id BIGINT,
  IN p_shop_id INT
)
BEGIN
  SELECT r.id_user,
         u.username,
         r.rating,
         r.title,
         r.p_comment,
         r.created_at
  FROM product_reviews r
  JOIN users u ON u.id_user = r.id_user
  WHERE r.product_id = p_product_id
    AND r.id_shop = p_shop_id
  ORDER BY r.created_at DESC;
END//
DELIMITER ;

-- upsert recensione 
DELIMITER //
CREATE PROCEDURE sp_upsert_review(
  IN p_product_id BIGINT,
  IN p_shop_id INT,
  IN p_user_id INT,
  IN p_rating INT,
  IN p_title VARCHAR(255),
  IN p_comment TEXT
)
BEGIN
  INSERT INTO product_reviews (product_id, id_shop, id_user, rating, title, p_comment)
  VALUES (p_product_id, p_shop_id, p_user_id, p_rating, NULLIF(TRIM(p_title), ''), NULLIF(TRIM(p_comment), ''))
  ON DUPLICATE KEY UPDATE
    rating     = VALUES(rating),
    title      = VALUES(title),
    p_comment  = VALUES(p_comment),
    created_at = CURRENT_TIMESTAMP;
END//
DELIMITER ;

-- SavedCardsDAO

-- Elenco carte utente
DELIMITER //
CREATE PROCEDURE sp_cards_find_by_user(IN p_user_id INT)
BEGIN
  SELECT card_id, holder, card_number, expiry, card_type
  FROM saved_cards
  WHERE id_user = p_user_id
  ORDER BY created_at DESC, card_id DESC;
END//
DELIMITER ;

-- Inserisce la carta se non esiste già
DELIMITER //
CREATE PROCEDURE sp_cards_insert_if_absent(
  IN  p_user_id     INT,
  IN  p_holder      VARCHAR(255),
  IN  p_card_number VARCHAR(64),
  IN  p_expiry      VARCHAR(16),
  IN  p_card_type   VARCHAR(32),
  OUT p_card_id     INT
)
BEGIN
  DECLARE v_norm VARCHAR(64);
  SET v_norm = REGEXP_REPLACE(IFNULL(p_card_number,''), '[^0-9]', '');

  IF EXISTS (
      SELECT 1
      FROM saved_cards
      WHERE id_user = p_user_id
        AND REGEXP_REPLACE(card_number, '[^0-9]', '') = v_norm
  ) THEN
    SET p_card_id = NULL;
  ELSE
    INSERT INTO saved_cards (id_user, holder, card_number, expiry, card_type)
    VALUES (p_user_id, p_holder, p_card_number, p_expiry, p_card_type);
    SET p_card_id = LAST_INSERT_ID();
  END IF;
END//
DELIMITER ;

-- Cancella carta 
DELIMITER //
CREATE PROCEDURE sp_cards_delete(
  IN  p_card_id INT,
  IN  p_user_id INT,
  OUT p_deleted TINYINT
)
BEGIN
  DELETE FROM saved_cards
  WHERE card_id = p_card_id AND id_user = p_user_id;
  SET p_deleted = (ROW_COUNT() > 0);
END//
DELIMITER ;

-- Aggiorna carta verificando che il numero normalizzato non sia duplicato in altre carte dello stesso utente
DELIMITER //
CREATE PROCEDURE sp_cards_update(
  IN  p_card_id     INT,
  IN  p_user_id     INT,
  IN  p_holder      VARCHAR(255),
  IN  p_card_number VARCHAR(64),
  IN  p_expiry      VARCHAR(16),
  IN  p_card_type   VARCHAR(32),
  OUT p_updated     TINYINT
)
BEGIN
  DECLARE v_norm VARCHAR(64);
  SET v_norm = REGEXP_REPLACE(IFNULL(p_card_number,''), '[^0-9]', '');

  IF EXISTS (
      SELECT 1
      FROM saved_cards
      WHERE id_user = p_user_id
        AND REGEXP_REPLACE(card_number, '[^0-9]', '') = v_norm
        AND card_id <> p_card_id
  ) THEN
    SET p_updated = 0;
  ELSE
    UPDATE saved_cards
    SET holder = p_holder,
        card_number = p_card_number,
        expiry = p_expiry,
        card_type = p_card_type
    WHERE card_id = p_card_id AND id_user = p_user_id;

    SET p_updated = (ROW_COUNT() > 0);
  END IF;
END//
DELIMITER ;

-- ShopDAO

-- Saldo negozio per userId
DELIMITER //
CREATE PROCEDURE sp_shop_get_balance_by_user(IN p_user_id INT)
BEGIN
  SELECT s.balance
  FROM shops s
  JOIN users u ON s.id_shop = u.id_shop
  WHERE u.id_user = p_user_id;
END//
DELIMITER ;

-- Richiesta prelievo: transazione con verifica saldo
DELIMITER //
CREATE PROCEDURE sp_shop_request_withdraw(
  IN p_user_id INT,
  IN p_amount  DECIMAL(18,2)
)
BEGIN
  IF p_amount IS NULL OR p_amount <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Importo non valido';
  END IF;

  START TRANSACTION;
    UPDATE shops s
    JOIN users u ON u.id_shop = s.id_shop
    SET s.balance = s.balance - p_amount
    WHERE u.id_user = p_user_id
      AND s.balance >= p_amount;

    IF ROW_COUNT() = 0 THEN
      ROLLBACK;
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Saldo insufficiente o shop non trovato';
    END IF;
  COMMIT;
END//
DELIMITER ;

-- Dati negozio per id_shop
DELIMITER //
CREATE PROCEDURE sp_shop_get_by_id(IN p_id_shop INT)
BEGIN
  SELECT id_shop, name_s, street, phone_number
  FROM shops
  WHERE id_shop = p_id_shop;
END//
DELIMITER ;

-- UserDAO

DELIMITER //

-- Login 
CREATE PROCEDURE sp_user_login(IN p_username VARCHAR(100))
BEGIN
  SELECT id_user, pass, rol
  FROM users
  WHERE username = p_username;
END//

-- Registrazione utente
CREATE PROCEDURE sp_user_register(
  IN p_username VARCHAR(100),
  IN p_pass     VARCHAR(200),
  IN p_role     VARCHAR(50),
  IN p_email    VARCHAR(200),
  IN p_phone    VARCHAR(50)
)
BEGIN
  INSERT INTO users(username, pass, rol, email, phone)
  VALUES(p_username, p_pass, p_role, p_email, p_phone);
END//

-- Controlli unicità
CREATE PROCEDURE sp_user_check_username(IN p_username VARCHAR(100))
BEGIN
  SELECT 1 FROM users WHERE username = p_username LIMIT 1;
END//

CREATE PROCEDURE sp_user_check_email(IN p_email VARCHAR(200))
BEGIN
  SELECT 1 FROM users WHERE email = p_email LIMIT 1;
END//

-- Trova id da username
CREATE PROCEDURE sp_user_find_id(IN p_username VARCHAR(100))
BEGIN
  SELECT id_user FROM users WHERE username = p_username;
END//

-- Profilo utente
CREATE PROCEDURE sp_user_find_by_username(IN p_username VARCHAR(100))
BEGIN
  SELECT username, email, phone, pass
  FROM users
  WHERE username = p_username;
END//

-- Aggiorna profilo senza password
CREATE PROCEDURE sp_user_update_profile(
  IN p_current_username VARCHAR(100),
  IN p_new_username     VARCHAR(100),
  IN p_email            VARCHAR(200),
  IN p_phone            VARCHAR(50)
)
BEGIN
  UPDATE users
  SET username = p_new_username,
      email    = p_email,
      phone    = p_phone
  WHERE username = p_current_username;
END//

-- Aggiorna profilo con password
CREATE PROCEDURE sp_user_update_profile_pwd(
  IN p_current_username VARCHAR(100),
  IN p_new_username     VARCHAR(100),
  IN p_email            VARCHAR(200),
  IN p_phone            VARCHAR(50),
  IN p_pass             VARCHAR(200)
)
BEGIN
  UPDATE users
  SET username = p_new_username,
      email    = p_email,
      phone    = p_phone,
      pass     = p_pass
  WHERE username = p_current_username;
END//

-- Aggiungi alla wishlist
CREATE PROCEDURE sp_wishlist_add(
  IN p_username VARCHAR(100),
  IN p_product_id BIGINT,
  IN p_shop_id INT,
  IN p_size VARCHAR(50)
)
BEGIN
  INSERT INTO wishlist(username, product_id, id_shop, p_size)
  VALUES(p_username, p_product_id, p_shop_id, p_size)
  ON DUPLICATE KEY UPDATE p_size = VALUES(p_size);
END//

-- Rimuovi da wishlist
CREATE PROCEDURE sp_wishlist_remove(
  IN p_username VARCHAR(100),
  IN p_product_id BIGINT,
  IN p_shop_id INT,
  IN p_size VARCHAR(50)
)
BEGIN
  DELETE FROM wishlist
  WHERE username = p_username
    AND product_id = p_product_id
    AND id_shop = p_shop_id
    AND p_size = p_size;
END//

-- Svuota wishlist
CREATE PROCEDURE sp_wishlist_clear(IN p_username VARCHAR(100))
BEGIN
  DELETE FROM wishlist WHERE username = p_username;
END//

-- Recupera wishlist
CREATE PROCEDURE sp_wishlist_get(IN p_username VARCHAR(100))
BEGIN
  SELECT p.product_id, p.name_p, p.sport, p.brand, p.category,
         w.id_shop, w.p_size, s.name_s, pa.price, p.image_data
  FROM wishlist w
  JOIN products p ON p.product_id = w.product_id
  JOIN shops s    ON s.id_shop    = w.id_shop
  LEFT JOIN product_availability pa
    ON pa.product_id = w.product_id
   AND pa.id_shop    = w.id_shop
   AND pa.size       = w.p_size
  WHERE w.username = p_username;
END//

DELIMITER ;

-- SellerDAO

DELIMITER //

-- Shop di un venditore
CREATE PROCEDURE sp_seller_find_shop(IN p_user_id INT)
BEGIN
  SELECT u.id_shop, s.name_s
  FROM users u
  LEFT JOIN shops s ON s.id_shop = u.id_shop
  WHERE u.id_user = p_user_id AND u.rol = 'venditore';
END//

-- Catalogo prodotti di uno shop
CREATE PROCEDURE sp_seller_list_catalog(IN p_shop_id INT, IN p_search VARCHAR(255))
BEGIN
  SELECT pa.product_id, p.name_p, p.sport, p.brand, p.category,
         pa.size, pa.price, pa.quantity
  FROM product_availability pa
  JOIN products p ON p.product_id = pa.product_id
  WHERE pa.id_shop = p_shop_id
    AND (p_search IS NULL
      OR p.name_p   LIKE CONCAT('%', p_search, '%')
      OR p.brand    LIKE CONCAT('%', p_search, '%')
      OR p.category LIKE CONCAT('%', p_search, '%')
      OR p.sport    LIKE CONCAT('%', p_search, '%'))
  ORDER BY p.name_p ASC, pa.size ASC;
END//

-- Upsert riga di catalogo
CREATE PROCEDURE sp_seller_upsert_catalog(
  IN p_shop_id INT,
  IN p_product_id INT,
  IN p_size VARCHAR(50),
  IN p_price DECIMAL(10,2),
  IN p_qty INT
)
BEGIN
  IF p_qty IS NULL OR p_qty <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quantità non valida';
  END IF;

  INSERT INTO product_availability (id_shop, product_id, size, price, quantity)
  VALUES (p_shop_id, p_product_id, p_size, p_price, p_qty)
  ON DUPLICATE KEY UPDATE
    quantity = quantity + VALUES(quantity);
END//

-- Update riga di catalogo
CREATE PROCEDURE sp_seller_update_catalog(
  IN p_shop_id INT,
  IN p_product_id INT,
  IN p_size VARCHAR(50),
  IN p_price DECIMAL(10,2),
  IN p_qty INT
)
BEGIN
  UPDATE product_availability
  SET price = p_price, quantity = p_qty
  WHERE id_shop = p_shop_id AND product_id = p_product_id AND size = p_size;
END//

-- Delete riga catalogo
CREATE PROCEDURE sp_seller_delete_catalog(
  IN p_shop_id INT,
  IN p_product_id INT,
  IN p_size VARCHAR(50)
)
BEGIN
  DELETE FROM product_availability
  WHERE id_shop = p_shop_id AND product_id = p_product_id AND size = p_size;
END//

-- Elenco opzioni prodotto
CREATE PROCEDURE sp_seller_list_all_product_options()
BEGIN
  SELECT product_id, name_p, brand, sport, category
  FROM products
  ORDER BY name_p;
END//

-- Ricerca opzioni prodotto per nome
CREATE PROCEDURE sp_seller_list_product_options_by_name(
  IN p_query VARCHAR(255),
  IN p_limit INT
)
BEGIN
  SELECT product_id, name_p, brand, sport, category
  FROM products
  WHERE name_p LIKE CONCAT('%', p_query, '%')
  ORDER BY name_p
  LIMIT p_limit;
END//

-- Ordini shop 
CREATE PROCEDURE sp_seller_list_shop_orders(
  IN p_shop_id INT,
  IN p_state VARCHAR(50)
)
BEGIN
  SELECT o.id_order,
         o.date_order,
         o.state_order,
         u.username AS customer,
         o.address,
         COALESCE(SUM(d.quantity * d.price), 0) AS total
  FROM orders_client o
  JOIN details_order d ON d.id_order = o.id_order AND d.id_shop = p_shop_id
  JOIN users u ON u.id_user = o.id_user
  WHERE (p_state IS NULL OR o.state_order = p_state)
  GROUP BY o.id_order, o.date_order, o.state_order, u.username, o.address
  ORDER BY o.date_order DESC, o.id_order DESC;
END//

-- Righe ordine per shop
CREATE PROCEDURE sp_seller_list_shop_order_lines(
  IN p_shop_id INT,
  IN p_order_id INT
)
BEGIN
  SELECT d.id_product,
         p.name_p AS product_name,
         d.size,
         d.quantity,
         d.price
  FROM details_order d
  JOIN products p ON p.product_id = d.id_product
  WHERE d.id_shop = p_shop_id AND d.id_order = p_order_id
  ORDER BY p.name_p ASC, d.id_product ASC;
END//

-- Update stato ordine
CREATE PROCEDURE sp_seller_update_order_state(
  IN p_order_id INT,
  IN p_new_state VARCHAR(50)
)
BEGIN
  UPDATE orders_client
  SET state_order = p_new_state,
      date_order_update = CURRENT_TIMESTAMP
  WHERE id_order = p_order_id;
END//

DELIMITER ;






