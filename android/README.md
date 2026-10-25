# CampusSchedule Android

Native Android WebView client for the existing Java/MariaDB application. Android 8.0 or later. Users enter their hosted HTTPS CampusSchedule base address (ending in `/Timetable/`), then use the existing registration, approval and login screens. No database credentials are embedded. This is an online client, not an offline database server.

Status: signed APK built on 22 October 2026 after user approval to install the SDK. Java compilation, APK packaging, alignment and v2/v3 signature verification passed. Android 8.0+ (API 26), target API 35. No physical-device or emulator runtime test has been performed. Hosting is still pending, so no live server URL is configured.

Build prerequisites: existing Java JDK plus Android platform 35 and SDK build tools. Compile resources/manifest with aapt2, Java sources against android.jar, convert classes using d8, assemble/zipalign/sign and verify the APK with apksigner. Retain the signing key privately for future updates. Do not distribute an unsigned APK or label this source folder as an APK.

WebView uses HTTPS only, no file/content access, no JavaScript native bridge, no SSL-error bypass, same-origin navigation and no third-party cookies. Changing the server clears WebView cookies and site storage. The client requests only internet access. It does not yet implement native PDF printing, downloads or offline mode.

Sources: https://developer.android.com/tools/sdkmanager and https://developer.android.com/privacy-and-security/risks/webview-unsafe-file-inclusion
