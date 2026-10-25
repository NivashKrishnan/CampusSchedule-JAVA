-- Structure only. Import into an EMPTY timetable_project database. No personal records or accounts included.
SET FOREIGN_KEY_CHECKS=0;
CREATE TABLE `activity_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(80) NOT NULL,
  `action` varchar(60) NOT NULL,
  `detail` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `app_settings` (
  `id` int(11) NOT NULL,
  `institution` varchar(80) NOT NULL,
  `academic_year` varchar(30) NOT NULL,
  `days` int(11) NOT NULL,
  `periods` int(11) NOT NULL,
  `start_time` varchar(5) NOT NULL,
  `period_minutes` int(11) NOT NULL,
  `break_after` int(11) NOT NULL,
  `break_minutes` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `classes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `department_id` int(11) NOT NULL,
  `student_count` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `department_id` (`department_id`),
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `classrooms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `capacity` int(11) NOT NULL,
  `type` varchar(12) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `departments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `faculty` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `max_hours` int(11) NOT NULL,
  `unavailable` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `saved_timetables` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `days` int(11) NOT NULL,
  `periods` int(11) NOT NULL,
  `lesson_count` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` varchar(12) NOT NULL DEFAULT 'draft',
  `start_time` varchar(5) NOT NULL DEFAULT '09:00',
  `period_minutes` int(11) NOT NULL DEFAULT 50,
  `break_after` int(11) NOT NULL DEFAULT 3,
  `break_minutes` int(11) NOT NULL DEFAULT 30,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subjects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  `class_id` int(11) NOT NULL,
  `faculty_id` int(11) NOT NULL,
  `weekly_hours` int(11) NOT NULL,
  `type` varchar(12) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `subject_class` (`name`,`class_id`),
  KEY `class_id` (`class_id`),
  KEY `faculty_id` (`faculty_id`),
  CONSTRAINT `subjects_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `subjects_ibfk_2` FOREIGN KEY (`faculty_id`) REFERENCES `faculty` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `timetable_entries` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `timetable_id` int(11) NOT NULL,
  `class_id` int(11) NOT NULL,
  `faculty_id` int(11) NOT NULL,
  `room_id` int(11) NOT NULL,
  `subject_id` int(11) NOT NULL,
  `class_name` varchar(80) NOT NULL,
  `faculty_name` varchar(80) NOT NULL,
  `room_name` varchar(80) NOT NULL,
  `subject_name` varchar(80) NOT NULL,
  `day_no` int(11) NOT NULL,
  `period_no` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `class_slot` (`timetable_id`,`class_id`,`day_no`,`period_no`),
  UNIQUE KEY `teacher_slot` (`timetable_id`,`faculty_id`,`day_no`,`period_no`),
  UNIQUE KEY `room_slot` (`timetable_id`,`room_id`,`day_no`,`period_no`),
  CONSTRAINT `timetable_entries_ibfk_1` FOREIGN KEY (`timetable_id`) REFERENCES `saved_timetables` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(80) NOT NULL,
  `salt` varchar(64) NOT NULL,
  `password_hash` varchar(128) NOT NULL,
  `role` varchar(12) NOT NULL DEFAULT 'admin',
  `faculty_id` int(11) DEFAULT NULL,
  `class_id` int(11) DEFAULT NULL,
  `active` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO app_settings VALUES(1,'Your college','2026-2027',6,8,'09:00',50,3,30);
