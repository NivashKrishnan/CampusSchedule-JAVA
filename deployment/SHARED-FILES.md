# Shared workspace without MariaDB

The existing Java servlet/JSP application supports a shared-file storage mode.
The Java scheduler and interface are retained. A private Vercel Blob object stores
the workspace, including salted password hashes. No SQL server is required in
this mode. The original JDBC implementation remains available when
`TIMETABLE_STORAGE` is unset.

## Hosting configuration

- `TIMETABLE_STORAGE=files` enables the functional shared workspace.
- `TIMETABLE_DEMO_MODE=false` disables the old read-only presentation redirect.
- Connect a **private** Blob store to the project. Vercel supplies `BLOB_STORE_ID`.
- Requests supply Vercel's short-lived OIDC token to the loopback-only storage
  bridge. No read-write token is committed or exposed to browsers.
- Set `TIMETABLE_SETUP_KEY` to an unpredictable secret of at least 32 characters
  in the Vercel project's sensitive Production environment variables. Do not put
  it in this repository. Redeploy after configuring it.
- Open `/Timetable/app`. On an empty store, enter the setup key and choose the
  first administrator username and a 12-100 character password. The owner must
  perform this credential entry privately. This endpoint refuses a second setup
  once an account exists. Remove the setup key from hosting after setup and
  redeploy. Removing it does not remove the account or saved records.
- Add academic records, generate and publish a timetable, and approve teacher
  and student registration requests. This store starts empty; no private laptop
  database data or credentials were automatically uploaded.

## Storage and authentication behavior

One versioned binary snapshot holds the workspace. It uses an explicitly bounded
data format, not Java object deserialization. Blob reads bypass the cache; writes
use ETag preconditions. Stale forms receive HTTP 409 and cannot overwrite another
user's changes. Failed writes are never reported as successful.

Production and preview use different object paths. A preview branch shares the
preview workspace, not the production workspace. The bridge listens only on
127.0.0.1:8181. Only Tomcat's port 8080 is exposed.

Authentication uses eight-hour signed, HttpOnly, SameSite=Strict cookies (Secure
and __Host-prefixed in cloud mode). Password changes, account disable and sign-out
revoke prior sessions across instances. Sign-out signs the user out on all devices.
CSRF tokens are required on every POST. Registration cannot create administrators;
new teacher/student accounts require administrator approval. Passwords use the
existing PBKDF2 implementation. The existing login-attempt limiter remains
per-process; this is not a distributed abuse-prevention service.

The 4 MiB snapshot bound and Blob free-tier operation/transfer limits make this
appropriate for a small student project. Every save rewrites the snapshot; high
write volume needs a different storage design. Quota/service errors return 503.
Do not describe this as unlimited or as providing database/JDBC connectivity.
The existing lab scheduler uses individual periods, not mandatory consecutive
multi-period labs. Clash checks apply within a generated timetable, not across
independent saved versions.

## Verification

Run `Build.ps1`, then `node tests/shared-files-check.mjs` on Windows. The harness
starts two isolated Tomcat instances and an on-disk conditional-write test server;
it does not touch MariaDB or the real Blob store. It covers all 15 workspace pages,
CRUD and linked records, owner setup, registration approval, roles, shared login,
stale writes, CSRF, full 48-period generation, teacher/room views, lesson clash and
availability checks, shared settings, duplicate/rename/publish/delete, CSV export,
password changes, session revocation and persistence after process restart.

Local result: **21 integration checks passed on 2026-11-01**. The Java build passed.
The original 14 scheduler constraint tests passed before the storage changes.
The private Blob store was created and connected on 2026-10-29.
Live container build, OIDC read/write and owner setup still need live verification.
No claim is made here that cloud login, mobile layout or physical-phone behavior
has been tested for this release.

References:
- https://vercel.com/docs/vercel-blob/using-blob-sdk
- https://vercel.com/docs/vercel-blob/usage-and-pricing
- https://vercel.com/docs/oidc
