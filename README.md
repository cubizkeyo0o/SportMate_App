# SportMate Android App

Native Android app using Java, XML Views, MVVM, LiveData, Navigation Component, RecyclerView, Hilt, Retrofit, and OkHttp. No Kotlin or Compose is used.

## Requirements

- JDK 17
- Android SDK Platform 36 and Android SDK Build Tools installed
- Android SDK Platform Tools (includes `adb`)
- Gradle Wrapper (included)

Android Studio is not required for daily development; install the SDK command-line tools and accept SDK licenses with `sdkmanager --licenses`.

## Architecture

Package `com.sportmate.app`:

```text
di/                      Hilt modules: NetworkModule (Gson, OkHttp, Retrofit, APIs), DataModule (bindings)
data/
  local/                 TokenStorage + SharedPreferences implementation
  remote/
    api/                 Retrofit interfaces, one per backend resource
    dto/                 Request/response DTOs matching the backend's snake_case JSON
    interceptor/         AuthInterceptor (adds the Bearer token)
    mapper/              DTO -> domain model mapping
    ApiCallExecutor      Runs calls, maps results, converts failures to AppError
    ApiErrorParser       Reads backend ProblemDetails error bodies
  repository/            Repository implementations
domain/
  common/                AppError, ResultCallback, Cancellable
  model/                 Domain models used by the UI (never DTOs)
  repository/            Repository interfaces
presentation/
  common/                UiState, Event, UiText, ErrorMessages, FormState, FormViews, UserInputRules
  session/               SessionManager (app-wide AuthState), SessionViewModel
  splash/ auth/ home/    One package per feature: Fragment + ViewModel
  profile/               Account details and sport profiles
```

Flow: Fragment -> ViewModel (LiveData) -> repository interface -> Retrofit. Fragments and ViewModels never call Retrofit directly.

Authentication: `SessionManager` holds the single `AuthState` (loading, unauthenticated, authenticated, error). At startup it validates a stored access token with `GET /api/v1/users/me`; a 401 clears the tokens. `MainActivity` is the only place that switches between `auth_nav_graph` (Welcome, Login, Register) and `main_nav_graph`, and both switches clear the back stack. Registering does not sign in (the backend returns no tokens), so it leads to Login. Logging out clears local tokens immediately and asks the backend to revoke the refresh token best-effort.

Profile (Home → My profile): shows email, role and account status read-only and edits display name and phone (`GET`/`PUT /api/v1/users/me`; an empty phone clears it). The sport list (`GET /api/v1/users/me/sports`) adds or changes a skill level with `PUT /api/v1/users/me/sports/{sportId}` and removes with `DELETE` on the same path; sports that can be added come from `GET /api/v1/sports` (active sports only). Client-side validation mirrors the backend's rules; the backend stays authoritative.

Tokens are stored in app-private SharedPreferences and excluded from backup and device transfer. Network logging is debug-only at `BASIC` level (request line and status, no headers or bodies); do not raise it to `BODY`, which would log passwords and tokens. Expired access tokens are not refreshed automatically yet.

## Build

From this directory in PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests and lint with `.\gradlew.bat testDebugUnitTest lintDebug`.

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The backend URL is `BuildConfig.API_BASE_URL`, set from the `sportmateApiBaseUrl` Gradle property (default `http://localhost:5000/`):

| Target | URL |
|---|---|
| Physical phone over USB with `adb reverse` | `http://localhost:5000/` (default) |
| Android emulator | `http://10.0.2.2:5000/` |
| Physical phone on the same Wi-Fi | `http://<computer-lan-ip>:5000/` |

Pass it per build, or set it once for your machine in `%USERPROFILE%\.gradle\gradle.properties` (outside the repository, so personal addresses are never committed):

```powershell
.\gradlew.bat assembleDebug -PsportmateApiBaseUrl=http://10.0.2.2:5000/
```

```properties
# %USERPROFILE%\.gradle\gradle.properties
sportmateApiBaseUrl=http://192.168.1.20:5000/
```

Debug builds permit cleartext HTTP for local development. Release builds disable cleartext HTTP and use a placeholder HTTPS endpoint that must be replaced before a real release.

## Physical phone over USB

1. On the phone, open Settings > About phone and tap Build number seven times to enable Developer Options (wording varies by manufacturer).
2. Open Developer Options and enable USB debugging.
3. Connect the phone to the development computer with a data-capable USB cable and approve the RSA debugging prompt on the phone.
4. From a terminal, verify the connection with `adb devices`; the phone should appear with status `device`.
5. Build the app with `.\gradlew.bat assembleDebug`.
6. Install it with `adb install .\app\build\outputs\apk\debug\app-debug.apk`.
7. Launch SportMate from the phone's app launcher. For subsequent installs use `adb install -r .\app\build\outputs\apk\debug\app-debug.apk`.

## Connect to the local backend

Start the backend on port 5000. For a USB connection, forward the port:

```powershell
adb reverse tcp:5000 tcp:5000
```

The debug default API URL is `http://localhost:5000/`, which reaches the computer through this reverse mapping. Remove the mapping with `adb reverse --remove tcp:5000` when finished.

For LAN instead, ensure the backend listens on `0.0.0.0:5000`, allow TCP 5000 through the computer firewall, and build with `-PsportmateApiBaseUrl=http://<computer-lan-ip>:5000/`. `API_BASE_URL` is defined once through the app's `BuildConfig`.