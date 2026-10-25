# CampusSchedule — Automated Timetable Scheduler

Java Servlet/JSP application with MariaDB JDBC connectivity and an Android client.

## Features

- Separate administrator, teacher and student login.
- Teacher/student registration with administrator approval.
- Departments, teachers, classes, rooms, labs and subject management.
- Timetable generation with weekly quotas and class/teacher/room clash checks.
- Full-week validation to prevent unassigned class periods.
- Saved draft/published timetables; teachers and students see only their linked records.

## Android download

[Download CampusSchedule 1.0 APK](https://github.com/NivashKrishnan/CampusSchedule-JAVA/raw/refs/heads/main/downloads/CampusSchedule-1.0.apk)

Android 8.0+. The signed APK is an online client: it needs a reachable HTTPS address for the Java application. Hosting is **not live yet**. Installing the APK alone does not start a server or connect to a laptop's localhost database. APK signature/alignment checks passed; device runtime testing is pending. Android source and build instructions are in `android/`.

## Java build

Use JDK 8 and Tomcat 9, with MariaDB 10.6-compatible SQL. The bundled JDBC driver is the existing project driver. On Windows, place JDK/Tomcat/MariaDB in a runtime directory and run:

```powershell
.\Build.ps1 -RuntimeHome 'C:\path\to\runtime'
.\Test.ps1 -RuntimeHome 'C:\path\to\runtime'
```

Deploy the generated `Timetable.war` to Tomcat. Set these environment variables privately in the server process:

| Variable | Purpose |
|---|---|
| `TIMETABLE_DB_URL` | JDBC URL for `timetable_project` |
| `TIMETABLE_DB_USER` | Dedicated database user |
| `TIMETABLE_DB_PASSWORD` | Database password from private server settings |
| `TIMETABLE_PUBLIC_DEPLOYMENT` | Set to `true` for online deployments |

The application does not read a `.env` file automatically. Never commit real credentials.

## Database and first administrator

`database/schema.sql` contains ten table definitions and generic application settings. Import it only into an empty `timetable_project` database. No real teacher/student records, saved schedules, existing account rows or password hashes are included in this repository.

For a fresh Windows setup with prerequisites already installed:

```powershell
.\Initialize-Local.ps1 -SqlExport .\database\schema.sql -RuntimeHome 'C:\path\to\runtime'
.\Start-Local.ps1
```

Initialization refuses to overwrite existing local state. Existing private data stays in the original local project and can be transferred separately to an authorized server.

Create the first administrator using `deployment/CreateAdmin.java`, supplying the database configuration plus `TIMETABLE_ADMIN_USERNAME` and `TIMETABLE_ADMIN_PASSWORD` as private environment variables. It refuses to overwrite an existing administrator. Remove the bootstrap password afterwards. See `deployment/ONLINE-SETUP.md` for the container command.

Teachers/students request accounts at `/Timetable/app?page=register`. An administrator verifies their linked class/teacher record and enables the account. Publish timetables before sharing them with these users.

## Hosting status

A Dockerfile is included. Hosting is pending. Oracle Always Free setup could not proceed because account/card verification is unavailable. No cloud resources or HTTPS URL have been created. A no-card alternative being evaluated is a Render free web service with TiDB Starter for a separate MySQL-compatible cloud database; compatibility testing and account setup are still required. The original local MariaDB database remains unchanged. Do not select paid resources. Earlier Railway deployment notes remain in `deployment/ONLINE-SETUP.md` as an optional paid/limited-credit alternative.

GitHub hosts the source and APK; it does not run this Java/MariaDB backend through GitHub Pages.

## Verification and limits

- Fourteen scheduler checks passed.
- Thirty registration/authentication/database checks passed against the local database, with test changes rolled back.
- JSP compilation and signed APK verification passed.
- Cloud deployment, container execution and physical-phone behavior remain unverified.
- Current sign-up limits: five attempts/hour per client IP; sign-in: thirty attempts/15 minutes. Shared networks share these limits.
- Admin-assisted password reset; no email reset service.
- Scheduling currently assigns one primary teacher per subject and does not enforce consecutive lab blocks.

Private runtime folders, database exports and Android signing material are excluded from Git.
