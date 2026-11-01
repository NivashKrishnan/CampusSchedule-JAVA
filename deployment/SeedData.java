package deployment;

import app.*;
import java.sql.*;
import java.security.SecureRandom;
import java.util.*;

public class SeedData {
    public static void main(String[] args) throws Exception {
        try (Connection c = Db.open()) {
            c.setAutoCommit(false);
            try {
                System.out.println("Starting database seeding...");

                // 1. App Settings
                Db.update(c, "UPDATE app_settings SET institution=?, academic_year=?, days=6, periods=8, start_time='09:00', period_minutes=50, break_after=3, break_minutes=30 WHERE id=1",
                    "Automated Campus Institute", "2026-2027");

                // 2. Clear old test data if any (preserve admin user)
                Db.update(c, "DELETE FROM timetable_entries");
                Db.update(c, "DELETE FROM saved_timetables");
                Db.update(c, "DELETE FROM subjects");
                Db.update(c, "DELETE FROM users WHERE role != 'admin'");
                Db.update(c, "DELETE FROM classes");
                Db.update(c, "DELETE FROM faculty");
                Db.update(c, "DELETE FROM classrooms");
                Db.update(c, "DELETE FROM departments");

                // 3. Departments
                int cseDept = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Computer Science & Engineering");
                int eceDept = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Electronics & Communication");
                int meDept  = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Mechanical Engineering");
                int ceDept  = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Civil Engineering");

                // 4. Faculty (max_hours = 24)
                int fSingh  = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Dr. O.N. Singh", 24, "");
                int fSharma = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Dr. S. K. Sharma", 24, "");
                int fTiwari = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Prof. A. K. Tiwari", 24, "");
                int fGupta  = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Dr. A. Gupta", 24, "");
                int fSkSingh= Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Dr. S. K. Singh", 24, "");
                int fTyagi  = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Prof. Tyagi", 24, "");
                int fPanda  = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Prof. Panda", 24, "");
                int fSks    = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Prof. SKS", 24, "");

                // 5. Classes
                int cCse = Db.insert(c, "INSERT INTO classes(name, department_id, student_count) VALUES(?, ?, ?)", "CSE(Ist-Year)", cseDept, 60);
                int cEce = Db.insert(c, "INSERT INTO classes(name, department_id, student_count) VALUES(?, ?, ?)", "ECE(Ist-Year)", eceDept, 60);


                // 6. Classrooms
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LH-101", 70, "Theory");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LH-102", 70, "Theory");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LH-201", 70, "Theory");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LH-202", 70, "Theory");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LAB-101 (CS Lab)", 65, "Lab");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "LAB-202 (Physics/EC Lab)", 65, "Lab");
                Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "WS-101 (Mechanical WS)", 65, "Lab");

                // 7. Subjects for CSE(Ist-Year) - 48 hours total
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Applied Physics", cCse, fSingh, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Applied Mathematics", cCse, fGupta, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Programming in C", cCse, fSharma, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Data Structures", cCse, fSharma, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Digital Logic", cCse, fSkSingh, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Professional Communication", cCse, fPanda, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Programming Lab", cCse, fSharma, 6, "Lab");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Physics Lab", cCse, fSingh, 6, "Lab");

                // 8. Subjects for ECE(Ist-Year) - 48 hours total
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Basic Electrical Engg", cEce, fTiwari, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Applied Mathematics II", cEce, fGupta, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Electronic Devices", cEce, fSkSingh, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Circuit Theory", cEce, fTiwari, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Environmental Studies", cEce, fTyagi, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Signals & Networks", cEce, fSingh, 6, "Theory");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Devices Lab", cEce, fSkSingh, 6, "Lab");
                Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Electrical Lab", cEce, fTiwari, 6, "Lab");

                // 9. Create Users (Teacher and Student)
                createUser(c, "faculty_singh", "Faculty@2026Singh", "teacher", fSingh, null);
                createUser(c, "faculty_sharma", "Faculty@2026Sharma", "teacher", fSharma, null);
                createUser(c, "student_cse", "Student@2026CSE", "student", null, cCse);
                createUser(c, "student_ece", "Student@2026ECE", "student", null, cEce);

                c.commit();
                System.out.println("Seeding completed successfully!");
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void createUser(Connection c, String username, String password, String role, Integer facultyId, Integer classId) throws Exception {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String salt = Base64.getEncoder().encodeToString(bytes);
        Db.update(c, "INSERT INTO users(username, salt, password_hash, role, faculty_id, class_id, active) VALUES(?, ?, ?, ?, ?, ?, 1)",
            username, salt, Security.hash(password, salt), role, facultyId, classId);
        System.out.println("Created " + role + " user: " + username);
    }
}
