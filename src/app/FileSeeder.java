package app;

import java.util.*;

/** Seeds the official B.Tech AI&DS (II/III) timetable into FileStore data. */
public final class FileSeeder {
    private static Map<String,String> row(String... pairs){
        Map<String,String> m=new LinkedHashMap<String,String>();
        for(int i=0;i<pairs.length;i+=2)m.put(pairs[i],pairs[i+1]);
        return m;
    }

    public static void seed(FileStore.Data d) {
        seed(d, false);
    }

    public static void seed(FileStore.Data d, boolean force) {
        if (!force && !d.rows("classes").isEmpty()) return;

        // Settings (6 days, 8 periods, 09:00 start, 50 mins, break after period 2 for 15 mins)
        d.settings.put("institution", "B.Tech AI & Data Science");
        d.settings.put("academic_year", "2026-2027");
        d.settings.put("days", "6");
        d.settings.put("periods", "8");
        d.settings.put("start_time", "09:00");
        d.settings.put("period_minutes", "50");
        d.settings.put("break_after", "2");
        d.settings.put("break_minutes", "15");

        // Clear existing timetable records (preserving users and activity log)
        d.rows("timetable_entries").clear();
        d.rows("saved_timetables").clear();
        d.rows("subjects").clear();
        d.rows("classes").clear();
        d.rows("faculty").clear();
        d.rows("classrooms").clear();
        d.rows("departments").clear();

        // Reset sequence IDs for clean predictable mapping
        d.meta.put("next.departments", "0");
        d.meta.put("next.faculty", "0");
        d.meta.put("next.classes", "0");
        d.meta.put("next.classrooms", "0");
        d.meta.put("next.subjects", "0");
        d.meta.put("next.saved_timetables", "0");
        d.meta.put("next.timetable_entries", "0");

        // 1. Departments (1-5)
        d.add("departments", row("name", "Artificial Intelligence & Data Science"));
        d.add("departments", row("name", "Computer Science & Engineering"));
        d.add("departments", row("name", "Information Technology"));
        d.add("departments", row("name", "Mathematics"));
        d.add("departments", row("name", "Placement & Training"));

        // 2. Faculty (1-10)
        d.add("faculty", row("name", "Mrs. R. Latha", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mr. V. Gopinath", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mrs. P. Rathika", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mrs. V. Sowmitha", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mrs. S. Pavithra", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mr. K. Vinoth", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mr. A. Manikandan", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mr. V. Karthi", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Mr. K. M. Giridharan", "max_hours", "24", "unavailable", ""));
        d.add("faculty", row("name", "Dr. Malatthi Sivasundaram", "max_hours", "24", "unavailable", ""));

        // 3. Classes (1)
        d.add("classes", row("name", "B.Tech/AI&DS (II/III)", "department_id", "1", "student_count", "60"));

        // 4. Classrooms (1-5)
        d.add("classrooms", row("name", "MIC F 216", "capacity", "70", "type", "Theory"));
        d.add("classrooms", row("name", "AJP Laboratory", "capacity", "65", "type", "Lab"));
        d.add("classrooms", row("name", "OS Laboratory", "capacity", "65", "type", "Lab"));
        d.add("classrooms", row("name", "DS Laboratory", "capacity", "65", "type", "Lab"));
        d.add("classrooms", row("name", "Design Studio Lab", "capacity", "65", "type", "Lab"));

        // 5. Subjects (1-12) totaling 48 hours
        d.add("subjects", row("name", "Discrete Mathematical Structures (DMS)", "class_id", "1", "faculty_id", "1", "weekly_hours", "7", "type", "Theory"));
        d.add("subjects", row("name", "Computer Organization & Architecture (COA)", "class_id", "1", "faculty_id", "2", "weekly_hours", "8", "type", "Theory"));
        d.add("subjects", row("name", "Data Structures (DS)", "class_id", "1", "faculty_id", "3", "weekly_hours", "5", "type", "Theory"));
        d.add("subjects", row("name", "Operating Systems (OS)", "class_id", "1", "faculty_id", "4", "weekly_hours", "6", "type", "Theory"));
        d.add("subjects", row("name", "Operating Systems Lab (OS LAB)", "class_id", "1", "faculty_id", "5", "weekly_hours", "2", "type", "Lab"));
        d.add("subjects", row("name", "Advanced Java Programming (AJP)", "class_id", "1", "faculty_id", "7", "weekly_hours", "4", "type", "Theory"));
        d.add("subjects", row("name", "Advanced Java Programming Lab (AJP LAB)", "class_id", "1", "faculty_id", "6", "weekly_hours", "6", "type", "Lab"));
        d.add("subjects", row("name", "Data Structures Lab (DS LAB)", "class_id", "1", "faculty_id", "3", "weekly_hours", "3", "type", "Lab"));
        d.add("subjects", row("name", "Design Studio Lab (DS-I LAB)", "class_id", "1", "faculty_id", "8", "weekly_hours", "3", "type", "Lab"));
        d.add("subjects", row("name", "Soft Skills Development-III (SSD-III)", "class_id", "1", "faculty_id", "9", "weekly_hours", "2", "type", "Theory"));
        d.add("subjects", row("name", "Mentor Hour (MH)", "class_id", "1", "faculty_id", "3", "weekly_hours", "1", "type", "Theory"));
        d.add("subjects", row("name", "Library / HOD Hour (LIB/HH)", "class_id", "1", "faculty_id", "10", "weekly_hours", "1", "type", "Theory"));

        // 6. Saved published timetable
        Map<String,String> run = d.add("saved_timetables", row(
            "name", "B.Tech/AI&DS (II/III) Official Timetable",
            "days", "6",
            "periods", "8",
            "lesson_count", "48",
            "status", "published",
            "created_at", java.time.Instant.now().toString(),
            "start_time", "09:00",
            "period_minutes", "50",
            "break_after", "2",
            "break_minutes", "15",
            "institution", "B.Tech AI & Data Science",
            "academic_year", "2026-2027"
        ));
        String tid = run.get("id");

        // 7. 48 Timetable Entries matching uploaded schedule
        addEntries(d, tid);

        // 8. Demo teacher and student accounts (preserves any existing admin user)
        seedUsers(d);
    }

    private static void addEntry(FileStore.Data d, String tid, int day, int period, String facultyId, String roomId, String subjectId, String subjectName) {
        String cName = d.find("classes", "1").get("name");
        String fName = d.find("faculty", facultyId).get("name");
        String rName = d.find("classrooms", roomId).get("name");
        d.add("timetable_entries", row(
            "timetable_id", tid,
            "class_id", "1",
            "faculty_id", facultyId,
            "room_id", roomId,
            "subject_id", subjectId,
            "class_name", cName,
            "faculty_name", fName,
            "room_name", rName,
            "subject_name", subjectName,
            "day_no", String.valueOf(day),
            "period_no", String.valueOf(period)
        ));
    }

    private static void addEntries(FileStore.Data d, String tid) {
        // DAY 1: MON
        addEntry(d, tid, 1, 1, "4", "1", "4", "OS - Operating Systems");
        addEntry(d, tid, 1, 2, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 1, 3, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 1, 4, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 1, 5, "2", "1", "2", "COA (CT: S1, S2)");
        addEntry(d, tid, 1, 6, "5", "3", "5", "OS LAB - Systems Lab");
        addEntry(d, tid, 1, 7, "5", "3", "5", "OS LAB - Systems Lab");
        addEntry(d, tid, 1, 8, "2", "1", "2", "COA (SSH)");

        // DAY 2: TUE
        addEntry(d, tid, 2, 1, "3", "1", "3", "DS - Data Structures");
        addEntry(d, tid, 2, 2, "3", "1", "3", "DS - Data Structures");
        addEntry(d, tid, 2, 3, "1", "1", "1", "DMS(SL) - Discrete Math");
        addEntry(d, tid, 2, 4, "1", "1", "1", "DMS(SL) - Discrete Math");
        addEntry(d, tid, 2, 5, "4", "1", "4", "OS (CT: S3, S4)");
        addEntry(d, tid, 2, 6, "2", "1", "2", "COA - Computer Organization");
        addEntry(d, tid, 2, 7, "2", "1", "2", "COA - Computer Organization");
        addEntry(d, tid, 2, 8, "3", "1", "3", "DS (SSH)");

        // DAY 3: WED
        addEntry(d, tid, 3, 1, "3", "1", "3", "DS - Data Structures");
        addEntry(d, tid, 3, 2, "7", "1", "6", "AJP(SL) - Java");
        addEntry(d, tid, 3, 3, "1", "1", "1", "DMS(T) - Discrete Math");
        addEntry(d, tid, 3, 4, "1", "1", "1", "DMS(T) - Discrete Math");
        addEntry(d, tid, 3, 5, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 3, 6, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 3, 7, "6", "2", "7", "AJP LAB - Advanced Java Lab");
        addEntry(d, tid, 3, 8, "4", "1", "4", "OS (SSH)");

        // DAY 4: THU
        addEntry(d, tid, 4, 1, "1", "1", "1", "DMS - Discrete Math");
        addEntry(d, tid, 4, 2, "2", "1", "2", "COA(SL)");
        addEntry(d, tid, 4, 3, "4", "1", "4", "OS - Operating Systems");
        addEntry(d, tid, 4, 4, "4", "1", "4", "OS - Operating Systems");
        addEntry(d, tid, 4, 5, "3", "4", "8", "DS LAB - Data Structures Lab");
        addEntry(d, tid, 4, 6, "3", "4", "8", "DS LAB - Data Structures Lab");
        addEntry(d, tid, 4, 7, "3", "4", "8", "DS LAB - Data Structures Lab");
        addEntry(d, tid, 4, 8, "1", "1", "1", "DMS (SSH)");

        // DAY 5: FRI
        addEntry(d, tid, 5, 1, "2", "1", "2", "COA - Computer Organization");
        addEntry(d, tid, 5, 2, "10", "1", "12", "LIB/HH - Library/HOD Hour");
        addEntry(d, tid, 5, 3, "9", "1", "10", "SSD - III (Soft Skills)");
        addEntry(d, tid, 5, 4, "9", "1", "10", "SSD - III (Soft Skills)");
        addEntry(d, tid, 5, 5, "8", "5", "9", "DS-I LAB - Design Studio");
        addEntry(d, tid, 5, 6, "8", "5", "9", "DS-I LAB - Design Studio");
        addEntry(d, tid, 5, 7, "8", "5", "9", "DS-I LAB - Design Studio");
        addEntry(d, tid, 5, 8, "7", "1", "6", "AJP (SSH)");

        // DAY 6: SAT
        addEntry(d, tid, 6, 1, "3", "1", "11", "MH - Mentor Hour");
        addEntry(d, tid, 6, 2, "4", "1", "4", "OS(SL) - Self Learning");
        addEntry(d, tid, 6, 3, "7", "1", "6", "AJP - Advanced Java");
        addEntry(d, tid, 6, 4, "2", "1", "2", "COA - Computer Organization");
        addEntry(d, tid, 6, 5, "1", "1", "1", "DMS - Discrete Math");
        addEntry(d, tid, 6, 6, "3", "1", "3", "DS(SL) - Self Learning");
        addEntry(d, tid, 6, 7, "7", "1", "6", "AJP - Advanced Java");
        addEntry(d, tid, 6, 8, "2", "1", "2", "COA (SSH) / Club Activities");
    }

    private static void seedUsers(FileStore.Data d) {
        try {
            seedUser(d, "latha", "Latha@2026Maths", "teacher", "1");
            seedUser(d, "gopinath", "Gopinath@2026AIDS", "teacher", "2");
            seedUser(d, "rathika", "Rathika@2026AIDS", "teacher", "3");
            seedUser(d, "sowmitha", "Sowmitha@2026CSE", "teacher", "4");
            seedUser(d, "vinoth", "Vinoth@2026AIDS", "teacher", "6");
            seedUser(d, "student_aids", "Student@2026AIDS", "student", "1");
        } catch (Exception ignored) {}
    }

    private static void seedUser(FileStore.Data d, String username, String password, String role, String linkedId) throws Exception {
        for (Map<String,String> u : d.rows("users")) {
            if (u.get("username").equalsIgnoreCase(username)) return;
        }
        byte[] b = new byte[24];
        new java.security.SecureRandom().nextBytes(b);
        String salt = Base64.getEncoder().encodeToString(b);
        Map<String,String> u = row(
            "username", username,
            "role", role,
            "active", "1",
            "faculty_id", role.equals("teacher") ? linkedId : "",
            "class_id", role.equals("student") ? linkedId : "",
            "salt", salt,
            "password_hash", Security.hash(password, salt),
            "version", Security.token()
        );
        d.add("users", u);
    }
}
