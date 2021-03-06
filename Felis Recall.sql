CREATE DATABASE felis_recall_db CHARACTER SET utf8mb4;

USE felis_recall_db;

CREATE TABLE fr_id_stub(
id BIGINT(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'user ID',
stub INT(3) NOT NULL DEFAULT 1  COMMENT 'stubNO.',
PRIMARY KEY id (id),
UNIQUE KEY stub (stub)
)ENGINE = InnoDB COMMENT 'Unique ID';

CREATE TABLE fr_user_login(
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
user_openid CHAR(28) NOT NULL UNIQUE COMMENT 'user WeChar openid',
user_state TINYINT(1) NOT NULL COMMENT 'user state: 0 -> offline  1 -> online',
last_login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'last login time',
PRIMARY KEY pk_user_id(user_id)
)ENGINE = InnoDB COMMENT 'user login table';

CREATE TABLE fr_user_register_info(
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
register_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'register time',
PRIMARY KEY pk_user_id(user_id)
)ENGINE = InnoDB COMMENT 'user register table';

CREATE TABLE fr_user_login_logs(
login_log_id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'login log ID',
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'login time',
PRIMARY KEY pk_login_log_id(login_log_id)
)ENGINE = InnoDB COMMENT 'user login logs table';

CREATE TABLE fr_user_info(
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
login_days INT(5) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'login days',
total_cards INT(5) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'total cards number',
forget_cards INT(5) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'forgeting cards number',
push_frequency TINYINT(2) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'push frequency',
PRIMARY KEY pk_user_id(user_id)
)ENGINE = InnoDB COMMENT 'user info table';

CREATE TABLE fr_card(
card_id BIGINT(16) NOT NULL COMMENT 'card ID',
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'creare time',
last_remember_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'last remember time',
next_time TIMESTAMP NOT NULL COMMENT 'next pushing time',
remember_times INT(3) NOT NULL COMMENT 'remember time counter and when it is not a positive number, it means user forget it',
title VARCHAR(20) COMMENT 'title',
label_num INT(1) NOT NULL DEFAULT 1 COMMENT 'the number of label: no more than 3',
content VARCHAR(200) COMMENT 'content',
pic_url VARCHAR(200) COMMENT 'picture url',
PRIMARY KEY pk_card_id(card_id),
INDEX next_time_idx(next_time)
)ENGINE = InnoDB COMMENT 'card info table';

CREATE TABLE fr_label(
label_id INT(3) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'label ID',
user_id BIGINT(11) NOT NULL COMMENT 'user ID',
label_content VARCHAR(20) NOT NULL DEFAULT 'Genaral' COMMENT 'label content',
PRIMARY KEY pk_label_id(label_id),
UNIQUE KEY uk_user_label(user_id, label_content)
)ENGINE = InnoDB COMMENT 'label table';

CREATE TABLE fr_label_map(
label_map_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'label map ID',
card_id BIGINT(16) NOT NULL COMMENT 'card ID',
label_id INT(3) UNSIGNED NOT NULL COMMENT 'label ID',
PRIMARY KEY pk_label_map_id(label_map_id)
)ENGINE = InnoDB COMMENT 'label map table';


REPLACE INTO fr_id_stub(stub) VALUES('1');
SELECT LAST_INSERT_ID();i