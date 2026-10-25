# Permanent website deployment

Status: application code and deployment files prepared. No cloud service or public link has been created. Docker is not installed on this laptop; the container build and cloud behavior still require verification on the selected host.

## Accounts

Open `/Timetable/app?page=register` to request a teacher or student account. Each person chooses a unique username and a password of at least 12 characters. New registrations are inactive. An administrator verifies the requested teacher/class association in **Login accounts and registration requests**, then selects **Approve / enable**. Public registration cannot create administrators.

Only published timetables are visible to approved teachers/students. The administrator should open timetable #12 and publish it when ready to share. Teacher accounts see their own lessons; student accounts see their own class. This is one shared college workspace, not separate colleges or independent administrator workspaces.

## Hosting choice and cost

Railway can build the included Dockerfile and provide an HTTPS domain. Use two services: the Java application and MariaDB with a persistent volume mounted at `/var/lib/mysql`. Do not use ephemeral storage for MariaDB. An account and an approved hosting budget are needed before creating services. The Hobby plan currently starts at $5/month including $5 of usage; total charges can exceed this. This is not a promise of free or uninterrupted hosting.

Sources checked 22 October 2026:
- https://docs.railway.com/builds/dockerfiles
- https://docs.railway.com/networking/public-networking
- https://docs.railway.com/volumes
- https://docs.railway.com/pricing/plans

## Deployment sequence

1. Sign in to the chosen hosting account. Confirm the plan, spending limit and database backups before provisioning.
2. Create a MariaDB service compatible with the existing 10.6 database. Create `timetable_project` and a dedicated application user. Keep MariaDB on the private service network; use an encrypted connection if crossing a public network.
3. Import the prepared private `cloud-import.sql` into the **empty cloud database only**. It contains the ten original table definitions and academic/timetable data. Existing account records, salts, password hashes and activity logs are excluded. No DROP statements are included. The import is for an empty target, not a merge or an update script. Confirm ten tables and timetable #12's 48 entries after import.
4. Deploy the application from the prepared source/Dockerfile. Do not upload `.local`, original backups or local credential files. Configure these private application environment variables:
   - `TIMETABLE_DB_URL`: `jdbc:mysql://<private-database-host>:3306/timetable_project?useUnicode=true&characterEncoding=UTF-8`
   - `TIMETABLE_DB_USER`: the dedicated cloud application user
   - `TIMETABLE_DB_PASSWORD`: its cloud password, entered in the host's secret settings
   - `TIMETABLE_PUBLIC_DEPLOYMENT=true` (already set in Dockerfile; blocks the local demo password)
5. Use one application replica, target port **8080**, and a generated HTTPS domain. The application path is `/Timetable/`. Keep the public endpoint HTTPS: the container sets Secure and HttpOnly session cookies. The container does not terminate TLS itself; the hosting edge does.
6. Create the first cloud administrator using the one-off helper below. Set `TIMETABLE_ADMIN_USERNAME` and a unique `TIMETABLE_ADMIN_PASSWORD` (12-100 characters) temporarily through private host settings. In the application container, after Tomcat has unpacked the WAR, run:

   ```sh
   java -cp '/opt/timetable-bootstrap:/usr/local/tomcat/webapps/Timetable/WEB-INF/classes:/usr/local/tomcat/webapps/Timetable/WEB-INF/lib/*' CreateAdmin
   ```

   The helper refuses to run if an admin already exists. It does not print the password. Remove both bootstrap environment variables afterwards. Never put credentials in the repository, command arguments or chat.
7. Validate the proxy chain and real client IP handling before sharing: `server.xml` uses Tomcat's default private proxy trust ranges. Auth throttling is per client IP, in memory, with five registration attempts/hour and thirty sign-in attempts/15 minutes. Shared campus networks share a limit; review these limits for a larger rollout. Use an edge limiter for multiple replicas.
8. Run cloud checks: registration → approval → login; wrong password/role rejection; teacher/class data isolation; draft visibility; publish #12; logout; phone/desktop layouts; database persistence after restarting both services. Confirm cookies are Secure, HttpOnly and have the expected SameSite behavior. Only share the domain once these checks pass.

## Current verification

Local Java build and JSP compilation passed. Thirty registration/authentication/database checks passed with test changes rolled back. The existing fourteen scheduling checks passed. Local account data and saved timetables were preserved. Browser interaction and the Docker/cloud deployment are not yet verified. This project uses admin-assisted password resets; it has no email verification or email reset service.
