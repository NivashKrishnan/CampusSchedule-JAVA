# Deploy the existing Java app on Vercel

The repository contains a Tomcat application, not a static HTML website. A
deployment can report Ready while returning Vercel's 404 page if no server was
built. The root JSP redirect only works when Tomcat is running.

The root `vercel.json` explicitly selects Vercel's Container framework and sets
the non-secret HTTP port to 8080, matching `deployment/server.xml`. Vercel should
build the existing root Dockerfile, which compiles Java and deploys Timetable.war
and the root redirect. No Java, UI, or database changes are needed for this setting.

## Dashboard checks

1. Use the repository root as the project's Root Directory.
2. Deploy the commit containing `vercel.json`. Redeploying an older commit will
   not include this fix.
3. Build logs should show a container image build and Java compilation. If an
   old custom Build Command or Output Directory override remains in dashboard
   settings, clear that override and use the Container preset's defaults.
4. If the account does not offer Container, stop and check availability with
   Vercel. Do not upgrade to a paid plan automatically.
5. After Ready, check `/`, `/Timetable/`, and `/Timetable/app`. The root should
   redirect into the app, and the login page should load.

## Database is a separate requirement

This deployment configuration does not create or connect a database. The current
Java application still requires its database for login, registration and saved
timetables. A cloud service cannot access the laptop's localhost MariaDB.
Keep database passwords only in the hosting dashboard's protected environment
settings; never put them in `vercel.json` or Git. For a demonstration with the
existing data, the original local Java/MariaDB setup remains available.

## Validation status

Configuration syntax and the port match can be checked locally. Docker is not
installed in the current workspace, so a successful Vercel container build and
live requests are still required before claiming the deployment works.

References:
- https://vercel.com/docs/functions/runtimes/container
- https://vercel.com/docs/project-configuration/vercel-json
