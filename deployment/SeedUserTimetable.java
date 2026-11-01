package deployment;

import app.*;
import java.sql.*;
import java.security.SecureRandom;
import java.util.*;

public class SeedUserTimetable {
    public static void main(String[] args) throws Exception {
        try (Connection c = Db.open()) {
            c.setAutoCommit(false);
            try {
                System.out.println("Applying timetable data from the uploaded image...");

                // 1. App Settings (8 periods, 6 days, start 09:00, 50 mins, break after period 2 for 15 mins)
                Db.update(c, "UPDATE app_settings SET institution=?, academic_year=?, days=6, periods=8, start_time='09:00', period_minutes=50, break_after=2, break_minutes=15 WHERE id=1",
                    "B.Tech AI & Data Science", "2026-2027");

                // 2. Clear old data while preserving admin
                Db.update(c, "DELETE FROM timetable_entries");
                Db.update(c, "DELETE FROM saved_timetables");
                Db.update(c, "DELETE FROM subjects");
                Db.update(c, "DELETE FROM users WHERE role != 'admin'");
                Db.update(c, "DELETE FROM classes");
                Db.update(c, "DELETE FROM faculty");
                Db.update(c, "DELETE FROM classrooms");
                Db.update(c, "DELETE FROM departments");

                // 3. Departments
                int deptAids  = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Artificial Intelligence & Data Science");
                int deptCse   = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Computer Science & Engineering");
                int deptIt    = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Information Technology");
                int deptMaths = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Mathematics");
                int deptPt    = Db.insert(c, "INSERT INTO departments(name) VALUES(?)", "Placement & Training");

                // 4. Faculty
                int fLatha      = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mrs. R. Latha", 24, "");
                int fGopinath   = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mr. V. Gopinath", 24, "");
                int fRathika    = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mrs. P. Rathika", 24, "");
                int fSowmitha   = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mrs. V. Sowmitha", 24, "");
                int fPavithra   = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mrs. S. Pavithra", 24, "");
                int fVinoth     = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mr. K. Vinoth", 24, "");
                int fManikandan = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mr. A. Manikandan", 24, "");
                int fKarthi     = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mr. V. Karthi", 24, "");
                int fGiridharan = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Mr. K. M. Giridharan", 24, "");
                int fMalatthi   = Db.insert(c, "INSERT INTO faculty(name, max_hours, unavailable) VALUES(?, ?, ?)", "Dr. Malatthi Sivasundaram", 24, "");

                // 5. Class
                String className = "B.Tech/AI&DS (II/III)";
                int classId = Db.insert(c, "INSERT INTO classes(name, department_id, student_count) VALUES(?, ?, ?)", className, deptAids, 60);

                // 6. Classrooms & Labs
                int rMic216 = Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "MIC F 216", 70, "Theory");
                int rAjpLab = Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "AJP Laboratory", 65, "Lab");
                int rOsLab  = Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "OS Laboratory", 65, "Lab");
                int rDsLab  = Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "DS Laboratory", 65, "Lab");
                int rDs1Lab = Db.insert(c, "INSERT INTO classrooms(name, capacity, type) VALUES(?, ?, ?)", "Design Studio Lab", 65, "Lab");

                // 7. Subjects (Sum of weekly hours = 48)
                int sDms   = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Discrete Mathematical Structures (DMS)", classId, fLatha, 7, "Theory");
                int sCoa   = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Computer Organization & Architecture (COA)", classId, fGopinath, 8, "Theory");
                int sDs    = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Data Structures (DS)", classId, fRathika, 5, "Theory");
                int sOs    = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Operating Systems (OS)", classId, fSowmitha, 6, "Theory");
                int sOsLab = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Operating Systems Lab (OS LAB)", classId, fPavithra, 2, "Lab");
                int sAjp   = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Advanced Java Programming (AJP)", classId, fManikandan, 4, "Theory");
                int sAjpLab= Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Advanced Java Programming Lab (AJP LAB)", classId, fVinoth, 6, "Lab");
                int sDsLab = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Data Structures Lab (DS LAB)", classId, fRathika, 3, "Lab");
                int sDs1Lab= Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Design Studio Lab (DS-I LAB)", classId, fKarthi, 3, "Lab");
                int sSsd3  = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Soft Skills Development-III (SSD-III)", classId, fGiridharan, 2, "Theory");
                int sMh    = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Mentor Hour (MH)", classId, fRathika, 1, "Theory");
                int sLib   = Db.insert(c, "INSERT INTO subjects(name, class_id, faculty_id, weekly_hours, type) VALUES(?, ?, ?, ?, ?)", "Library / HOD Hour (LIB/HH)", classId, fMalatthi, 1, "Theory");

                // 8. Saved Timetable record
                int timetableId = Db.insert(c, "INSERT INTO saved_timetables(name, days, periods, lesson_count, status, start_time, period_minutes, break_after, break_minutes) VALUES(?, 6, 8, 48, 'published', '09:00', 50, 2, 15)",
                    "B.Tech/AI&DS (II/III) Official Timetable");

                // 9. Exact 48 timetable entries from the document:
                // Helper lambda / method to insert an entry
                class EntryHelper {
                    void add(int day, int period, int facultyId, String facultyName, int roomId, String roomName, int subjectId, String subjectName) throws SQLException {
                        Db.update(c, "INSERT INTO timetable_entries(timetable_id, class_id, faculty_id, room_id, subject_id, class_name, faculty_name, room_name, subject_name, day_no, period_no) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            timetableId, classId, facultyId, roomId, subjectId, className, facultyName, roomName, subjectName, day, period);
                    }
                }
                EntryHelper eh = new EntryHelper();

                // DAY 1: MON
                eh.add(1, 1, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS - Operating Systems");
                eh.add(1, 2, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(1, 3, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(1, 4, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(1, 5, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA (CT: S1, S2)");
                eh.add(1, 6, fPavithra, "Mrs. S. Pavithra", rOsLab, "OS Laboratory", sOsLab, "OS LAB - Systems Lab");
                eh.add(1, 7, fPavithra, "Mrs. S. Pavithra", rOsLab, "OS Laboratory", sOsLab, "OS LAB - Systems Lab");
                eh.add(1, 8, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA (SSH)");

                // DAY 2: TUE
                eh.add(2, 1, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sDs, "DS - Data Structures");
                eh.add(2, 2, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sDs, "DS - Data Structures");
                eh.add(2, 3, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS(SL) - Discrete Math");
                eh.add(2, 4, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS(SL) - Discrete Math");
                eh.add(2, 5, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS (CT: S3, S4)");
                eh.add(2, 6, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA - Computer Organization");
                eh.add(2, 7, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA - Computer Organization");
                eh.add(2, 8, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sDs, "DS (SSH)");

                // DAY 3: WED
                eh.add(3, 1, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sDs, "DS - Data Structures");
                eh.add(3, 2, fManikandan, "Mr. A. Manikandan", rMic216, "MIC F 216", sAjp, "AJP(SL) - Java");
                eh.add(3, 3, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS(T) - Discrete Math");
                eh.add(3, 4, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS(T) - Discrete Math");
                eh.add(3, 5, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(3, 6, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(3, 7, fVinoth, "Mr. K. Vinoth", rAjpLab, "AJP Laboratory", sAjpLab, "AJP LAB - Advanced Java Lab");
                eh.add(3, 8, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS (SSH)");

                // DAY 4: THU
                eh.add(4, 1, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS - Discrete Math");
                eh.add(4, 2, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA(SL)");
                eh.add(4, 3, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS - Operating Systems");
                eh.add(4, 4, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS - Operating Systems");
                eh.add(4, 5, fRathika, "Mrs. P. Rathika", rDsLab, "DS Laboratory", sDsLab, "DS LAB - Data Structures Lab");
                eh.add(4, 6, fRathika, "Mrs. P. Rathika", rDsLab, "DS Laboratory", sDsLab, "DS LAB - Data Structures Lab");
                eh.add(4, 7, fRathika, "Mrs. P. Rathika", rDsLab, "DS Laboratory", sDsLab, "DS LAB - Data Structures Lab");
                eh.add(4, 8, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS (SSH)");

                // DAY 5: FRI
                eh.add(5, 1, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA - Computer Organization");
                eh.add(5, 2, fMalatthi, "Dr. Malatthi Sivasundaram", rMic216, "MIC F 216", sLib, "LIB/HH - Library/HOD Hour");
                eh.add(5, 3, fGiridharan, "Mr. K. M. Giridharan", rMic216, "MIC F 216", sSsd3, "SSD - III (Soft Skills)");
                eh.add(5, 4, fGiridharan, "Mr. K. M. Giridharan", rMic216, "MIC F 216", sSsd3, "SSD - III (Soft Skills)");
                eh.add(5, 5, fKarthi, "Mr. V. Karthi", rDs1Lab, "Design Studio Lab", sDs1Lab, "DS-I LAB - Design Studio");
                eh.add(5, 6, fKarthi, "Mr. V. Karthi", rDs1Lab, "Design Studio Lab", sDs1Lab, "DS-I LAB - Design Studio");
                eh.add(5, 7, fKarthi, "Mr. V. Karthi", rDs1Lab, "Design Studio Lab", sDs1Lab, "DS-I LAB - Design Studio");
                eh.add(5, 8, fManikandan, "Mr. A. Manikandan", rMic216, "MIC F 216", sAjp, "AJP (SSH)");

                // DAY 6: SAT
                eh.add(6, 1, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sMh, "MH - Mentor Hour");
                eh.add(6, 2, fSowmitha, "Mrs. V. Sowmitha", rMic216, "MIC F 216", sOs, "OS(SL) - Self Learning");
                eh.add(6, 3, fManikandan, "Mr. A. Manikandan", rMic216, "MIC F 216", sAjp, "AJP - Advanced Java");
                eh.add(6, 4, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA - Computer Organization");
                eh.add(6, 5, fLatha, "Mrs. R. Latha", rMic216, "MIC F 216", sDms, "DMS - Discrete Math");
                eh.add(6, 6, fRathika, "Mrs. P. Rathika", rMic216, "MIC F 216", sDs, "DS(SL) - Self Learning");
                eh.add(6, 7, fManikandan, "Mr. A. Manikandan", rMic216, "MIC F 216", sAjp, "AJP - Advanced Java");
                eh.add(6, 8, fGopinath, "Mr. V. Gopinath", rMic216, "MIC F 216", sCoa, "COA (SSH) / Club Activities");

                // 10. Login accounts
                createUser(c, "latha", "Latha@2026Maths", "teacher", fLatha, null);
                createUser(c, "gopinath", "Gopinath@2026AIDS", "teacher", fGopinath, null);
                createUser(c, "rathika", "Rathika@2026AIDS", "teacher", fRathika, null);
                createUser(c, "sowmitha", "Sowmitha@2026CSE", "teacher", fSowmitha, null);
                createUser(c, "vinoth", "Vinoth@2026AIDS", "teacher", fVinoth, null);
                createUser(c, "student_aids", "Student@2026AIDS", "student", null, classId);

                c.commit();
                System.out.println("Timetable data imported and verified successfully!");
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

