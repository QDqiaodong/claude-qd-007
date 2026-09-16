-- 养老院：建表 + 种子数据
-- 表结构由 Hibernate 兜底（ddl-auto=update），这里只保证首次启动就有数据

CREATE TABLE IF NOT EXISTS room (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  floor INT NOT NULL DEFAULT 1,
  kind VARCHAR(16) NOT NULL DEFAULT '双人间',
  capacity INT NOT NULL DEFAULT 2,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bed (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  room_id BIGINT NULL,
  position VARCHAR(16) NOT NULL DEFAULT '中间',
  status VARCHAR(16) NOT NULL DEFAULT '空闲',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bed_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resident (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(32) NOT NULL,
  gender VARCHAR(8) NOT NULL DEFAULT '女',
  age INT NOT NULL DEFAULT 60,
  care_level VARCHAR(16) NOT NULL DEFAULT '自理',
  room_id BIGINT NULL,
  bed_id BIGINT NULL,
  check_in_date DATE NULL,
  check_out_date DATE NULL,
  family_phone VARCHAR(20) NULL,
  status VARCHAR(16) NOT NULL DEFAULT '在住',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resident_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS care_shift (
  id BIGINT NOT NULL AUTO_INCREMENT,
  shift_no VARCHAR(32) NOT NULL,
  room_id BIGINT NOT NULL,
  shift_date DATE NOT NULL,
  period VARCHAR(16) NOT NULL DEFAULT '早班',
  nurse VARCHAR(32) NOT NULL,
  start_min INT NOT NULL,
  end_min INT NOT NULL,
  handover_note VARCHAR(255) NULL,
  status VARCHAR(16) NOT NULL DEFAULT '待接班',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_care_shift_no (shift_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medicine (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  unit VARCHAR(16) NOT NULL DEFAULT '片',
  kind VARCHAR(16) NOT NULL DEFAULT '非处方',
  stock INT NOT NULL DEFAULT 0,
  warn_stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_medicine_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS medicine_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resident_id BIGINT NOT NULL,
  medicine_id BIGINT NOT NULL,
  qty INT NOT NULL,
  kind VARCHAR(16) NOT NULL DEFAULT '发放',
  dose_time VARCHAR(8) NOT NULL DEFAULT '早',
  issue_date DATE NOT NULL,
  operator VARCHAR(32) NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ 种子数据 ============

INSERT IGNORE INTO room (id, code, name, floor, kind, capacity, status) VALUES
  (1, 'R-101', '一区101', 1, '双人间', 2, '在用'),
  (2, 'R-102', '一区102', 1, '双人间', 2, '在用'),
  (3, 'R-103', '一区103', 1, '单人间', 1, '在用'),
  (4, 'R-201', '二区201', 2, '多人间', 4, '在用'),
  (5, 'R-301', '三区301', 3, '双人间', 2, '维修');

INSERT IGNORE INTO bed (id, code, room_id, position, status) VALUES
  (1, 'BD-1011', 1, '靠窗', '占用'),
  (2, 'BD-1012', 1, '靠门', '占用'),
  (3, 'BD-1021', 2, '靠窗', '空闲'),
  (4, 'BD-1022', 2, '靠门', '空闲'),
  (5, 'BD-1031', 3, '靠窗', '占用'),
  (6, 'BD-2011', 4, '靠窗', '空闲'),
  (7, 'BD-2012', 4, '靠门', '停用'),
  (8, 'BD-3011', 5, '靠窗', '空闲');

INSERT IGNORE INTO resident (id, code, name, gender, age, care_level, room_id, bed_id, check_in_date, check_out_date, family_phone, status, created_at, updated_at) VALUES
  (1, 'LA-0001', '张桂芳', '女', 82, '半自理',   1, 1,    DATE_SUB(CURDATE(), INTERVAL 200 DAY), NULL, '13800000001', '在住',     NOW(), NOW()),
  (2, 'LA-0002', '李长海', '男', 79, '自理',     1, 2,    DATE_SUB(CURDATE(), INTERVAL 150 DAY), NULL, '13800000002', '在住',     NOW(), NOW()),
  (3, 'LA-0003', '王秀英', '女', 88, '不能自理', 3, 5,    DATE_SUB(CURDATE(), INTERVAL 320 DAY), NULL, '13800000003', '在住',     NOW(), NOW()),
  (4, 'LA-0004', '陈建国', '男', 75, '自理',     2, NULL, DATE_SUB(CURDATE(), INTERVAL 400 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), '13800000004', '已退住', NOW(), NOW()),
  (5, 'LA-0005', '赵秀兰', '女', 84, '半自理',   4, NULL, DATE_SUB(CURDATE(), INTERVAL 90 DAY),  NULL, '13800000005', '请假外出', NOW(), NOW()),
  (6, 'LA-0006', '孙德明', '男', 81, '自理',     2, NULL, DATE_SUB(CURDATE(), INTERVAL 260 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY), '13800000006', '已退住', NOW(), NOW());

INSERT IGNORE INTO care_shift (id, shift_no, room_id, shift_date, period, nurse, start_min, end_min, handover_note, status, created_at, updated_at) VALUES
  (1, 'HS-0001', 1, CURDATE(), '早班', '王护士', 420, 540, NULL, '值班中', NOW(), NOW()),
  (2, 'HS-0002', 2, CURDATE(), '早班', '李护士', 420, 540, NULL, '待接班', NOW(), NOW()),
  (3, 'HS-0003', 1, CURDATE(), '中班', '张护士', 780, 900, NULL, '待接班', NOW(), NOW()),
  (4, 'HS-0004', 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '早班', '王护士', 300, 420, '夜间咳得厉害，已提醒家属', '已交班', NOW(), NOW()),
  (5, 'HS-0005', 5, CURDATE(), '早班', '赵护士', 420, 540, NULL, '已取消', NOW(), NOW()),
  (6, 'HS-0006', 4, CURDATE(), '早班', '钱护士', 600, 720, NULL, '待接班', NOW(), NOW());

INSERT IGNORE INTO medicine (id, code, name, unit, kind, stock, warn_stock, status) VALUES
  (1, 'MD-1001', '阿司匹林肠溶片', '片', '非处方', 60, 20, '在用'),
  (2, 'MD-1002', '二甲双胍片',     '片', '处方药', 30, 10, '在用'),
  (3, 'MD-1003', '苯磺酸氨氯地平', '片', '处方药', 12,  5, '在用'),
  (4, 'MD-1004', '碳酸钙D3片',     '片', '非处方',  0, 10, '在用'),
  (5, 'MD-1005', '艾司唑仑片',     '片', '处方药', 25,  5, '停用'),
  (6, 'MD-1006', '感冒灵颗粒',     '袋', '非处方', 40, 15, '在用');

INSERT IGNORE INTO medicine_issue (id, resident_id, medicine_id, qty, kind, dose_time, issue_date, operator, created_at) VALUES
  (1, 1, 1, 2, '发放', '早', CURDATE(), '王护士', NOW()),
  (2, 1, 1, 3, '发放', '中', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '王护士', NOW()),
  (3, 3, 3, 1, '发放', '晚', CURDATE(), '李护士', NOW()),
  (4, 3, 3, 1, '退回', '晚', CURDATE(), '李护士', NOW()),
  (5, 2, 1, 4, '发放', '中', CURDATE(), '张护士', NOW()),
  (6, 1, 2, 2, '发放', '早', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '王护士', NOW()),
  (7, 1, 2, 1, '退回', '中', CURDATE(), '王护士', NOW());
