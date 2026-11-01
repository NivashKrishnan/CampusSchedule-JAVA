-- Seed data for B.Tech AI&DS (II Year / III Semester) Timetable
-- Hall: MIC F 216

UPDATE `app_settings` SET 
  `institution` = 'B.Tech AI & Data Science',
  `academic_year` = '2026-2027',
  `days` = 6,
  `periods` = 8,
  `start_time` = '09:00',
  `period_minutes` = 50,
  `break_after` = 2,
  `break_minutes` = 15
WHERE `id` = 1;

-- Departments
INSERT INTO `departments` (`id`, `name`) VALUES
(1, 'Artificial Intelligence & Data Science'),
(2, 'Computer Science & Engineering'),
(3, 'Information Technology'),
(4, 'Mathematics'),
(5, 'Placement & Training')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- Faculty
INSERT INTO `faculty` (`id`, `name`, `max_hours`, `unavailable`) VALUES
(1, 'Mrs. R. Latha', 24, ''),
(2, 'Mr. V. Gopinath', 24, ''),
(3, 'Mrs. P. Rathika', 24, ''),
(4, 'Mrs. V. Sowmitha', 24, ''),
(5, 'Mrs. S. Pavithra', 24, ''),
(6, 'Mr. K. Vinoth', 24, ''),
(7, 'Mr. A. Manikandan', 24, ''),
(8, 'Mr. V. Karthi', 24, ''),
(9, 'Mr. K. M. Giridharan', 24, ''),
(10, 'Dr. Malatthi Sivasundaram', 24, '')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `max_hours`=VALUES(`max_hours`);

-- Classes
INSERT INTO `classes` (`id`, `name`, `department_id`, `student_count`) VALUES
(1, 'B.Tech/AI&DS (II/III)', 1, 60)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- Classrooms
INSERT INTO `classrooms` (`id`, `name`, `capacity`, `type`) VALUES
(1, 'MIC F 216', 70, 'Theory'),
(2, 'AJP Laboratory', 65, 'Lab'),
(3, 'OS Laboratory', 65, 'Lab'),
(4, 'DS Laboratory', 65, 'Lab'),
(5, 'Design Studio Lab', 65, 'Lab')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- Subjects
INSERT INTO `subjects` (`id`, `name`, `class_id`, `faculty_id`, `weekly_hours`, `type`) VALUES
(1, 'Discrete Mathematical Structures (DMS)', 1, 1, 7, 'Theory'),
(2, 'Computer Organization & Architecture (COA)', 1, 2, 8, 'Theory'),
(3, 'Data Structures (DS)', 1, 3, 5, 'Theory'),
(4, 'Operating Systems (OS)', 1, 4, 6, 'Theory'),
(5, 'Operating Systems Lab (OS LAB)', 1, 5, 2, 'Lab'),
(6, 'Advanced Java Programming (AJP)', 1, 7, 4, 'Theory'),
(7, 'Advanced Java Programming Lab (AJP LAB)', 1, 6, 6, 'Lab'),
(8, 'Data Structures Lab (DS LAB)', 1, 3, 3, 'Lab'),
(9, 'Design Studio Lab (DS-I LAB)', 1, 8, 3, 'Lab'),
(10, 'Soft Skills Development-III (SSD-III)', 1, 9, 2, 'Theory'),
(11, 'Mentor Hour (MH)', 1, 3, 1, 'Theory'),
(12, 'Library / HOD Hour (LIB/HH)', 1, 10, 1, 'Theory')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `weekly_hours`=VALUES(`weekly_hours`);
