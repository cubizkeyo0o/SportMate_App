# SportMate Android App

Native Android starter using Java, XML Views, MVVM, LiveData, Navigation Component, RecyclerView, Hilt, Retrofit, and OkHttp. The only initial screen displays SportMate. No Kotlin or Compose is used.

## Requirements

- JDK 17
- Android SDK Platform 36 and Android SDK Build Tools installed
- Android SDK Platform Tools (includes `adb`)
- Gradle Wrapper (included)

Android Studio is not required for daily development; install the SDK command-line tools and accept SDK licenses with `sdkmanager --licenses`.

## Build

From this directory in PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Override the development API URL centrally at build time with:

```powershell
.\gradlew.bat assembleDebug -PsportmateApiBaseUrl=http://192.168.1.20:5000/
```

Replace the sample LAN address with the development computer's address. Debug builds permit cleartext HTTP for local development. Release builds disable cleartext HTTP and use a placeholder HTTPS endpoint that must be replaced before a real release.

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