# Aminul MultiBook

**Created by Aminulsordar**  
*Multiple Accounts, One App*

---

## 1. Project Overview

**Aminul MultiBook** is a lightweight, production-grade Android application inspired by Facebook Lite that allows users to add and manage multiple Facebook accounts simultaneously within a single native Android client.

The application achieves **true browser session isolation** by leveraging the **AndroidX WebKit Multi-Profile Architecture** (`ProfileStore` & `WebViewCompat.setProfile`), ensuring each saved Facebook account maintains its own completely independent:
- Authentication cookies (`profile.cookieManager`)
- Web Storage, LocalStorage & IndexedDB (`profile.webStorage`)
- Session cookies & HTTP authentication state
- Browser cache & offline storage
- Independent login and logout flows (logging out of Account A does NOT affect Account B)

---

## 2. Technology Stack & Identity

- **Developer**: Aminulsordar
- **Application Name**: Aminul MultiBook
- **Package**: `com.aminul.multibook`
- **Target Platform**: Android 8.0 (API level 26) through Android 15+ (API level 36)
- **UI Architecture**: Jetpack Compose with Material Design 3 (M3)
- **Custom Branding**: Professional adaptive multi-card browser icon in electric blue, white, and dark navy
- **Splash Screen**: Animated branded splash screen with "Created by Aminulsordar" signature badge
- **Extensions**: Integrated `c3c-fbstate` extension suite for exporting and importing Facebook session states (`fbstate.json`), with AES-256 CTR encryption/decryption and safe logout support.
- **Desktop Mode**: Full PC user-agent switching, wide responsive viewport, zoom controls, and immersive fullscreen browsing
- **Session Isolation**: AndroidX WebKit `ProfileStore` and per-account Profile Management
- **Local Database**: Android Jetpack Room Database with Kotlin Coroutines and KSP

---

## 3. c3c-fbstate Extension Integration

The application natively integrates the **c3c-fbstate** extension functionality:
- **Export FBState**: Extracts the current account's cookies into standard `fbstate.json` format for use with bots (C3C, fca-unofficial) or session backup.
- **Import FBState**: Injects `fbstate.json` directly into the account's isolated profile and reloads the browser, logging in instantly without typing credentials.
- **AES-256 CTR Encryption / Decryption**: Full compatibility with `c3c-fbstate` AES-CTR mode with SHA-256 key derivation, allowing password-protected encrypted exports and imports.
- **Safe Logout (not die fbstate)**: Removes transient session tokens while preserving `sb` and `dbln` cookies so Facebook recognizes the profile device.
- **Access Points**: Directly accessible via the browser toolbar (Extension puzzle icon) and from each Account Card's 3-dot menu on the Home screen.
- **Bundled Assets**: Source extension assets are bundled in `assets/extensions/c3c_fbstate/` inside the APK.

---

## 4. Desktop Mode & Advanced Browsing Features

- **Desktop Mode Toggle in Settings**: Choose whether accounts open by default in Mobile Mode (`m.facebook.com`) or Desktop Mode (`www.facebook.com`).
- **Instant In-Browser Switching**: Tap the Desktop/Mobile icon directly in the browser toolbar to switch modes on the fly.
- **Zero Session Loss**: Switching between Mobile and Desktop modes preserves all login cookies and sessions seamlessly because both modes operate on the same isolated account profile.
- **PC User-Agent Switching**: Emulates modern PC Chrome on Windows 10/11 (`Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...`).
- **Zoom Controls**: Adjust zoom (`+15%`, `-15%`, reset to `100%`) directly from the browser toolbar, in addition to native pinch-to-zoom.
- **Immersive Fullscreen Mode**: Hide system bars and app headers to maximize screen real estate, complete with a floating quick-exit button.

---

## 5. Developer Mode & Diagnostics Dashboard

Enabled via **Settings > Developer Mode**, unlocking an advanced engineering console:
- **App & Android Platform Information**: Displays App Name, Developer (`Aminulsordar`), Package Name, Version 1.0 (Code 1), Android OS Version, Device Model, Manufacturer, Board/Hardware, Supported ABIs, and System Uptime.
- **Live RAM & Memory Telemetry**: System RAM metrics (Total, Available, Low Memory state) and JVM Heap allocations (Allocated Heap, Free Heap, Max Heap) with manual Garbage Collector trigger.
- **WebView Engine Status**: Shows installed WebView provider package, version, multi-profile capability, app data directory, and cache directory.
- **Interactive Isolation Test Runner**: Built-in 4-stage automated test suite that verifies `ProfileStore` availability, creates isolated test profiles, and verifies cookie segregation boundaries.
- **Cache & Storage Reset**: On-demand tools to clear HTTP disk cache, wipe WebStorage, and reset global cookie jars.
- **Live Diagnostic Logs Viewer**: In-memory rolling event logger capturing session transitions, profile loads, and system diagnostics.

---

## 6. Testing Instructions

### Test Case 1: c3c-fbstate Extension (Export / Import)
1. Open an account in the browser.
2. Tap the **Extension** icon (puzzle icon) in the top toolbar.
3. In the **Export** tab:
   - Tap **Plain JSON** -> Verify the formatted `fbstate.json` array appears.
   - Tap **Copy** to copy to clipboard or **Share/Save** to share the file.
   - Select **Encrypted**, enter a password, and verify the encrypted hex string is generated.
4. In the **Import** tab:
   - Paste an `fbstate.json` string or tap **Select File** to pick a `.json` file.
   - Tap **Import & Log In Instantly** -> Verify cookies are injected into the isolated profile and the WebView reloads authenticated!
5. Tap **Logout (not die fbstate)** -> Verify safe logout removes session cookies while keeping device identifiers.

### Test Case 2: Desktop Mode & Seamless Switching
1. Tap the **Desktop Site** icon in the toolbar.
2. Verify the URL switches to `www.facebook.com` and renders the desktop layout.
3. Verify you remain **completely logged in** without losing session.

### Test Case 3: In-Browser Zoom Controls
1. While browsing, tap the **Zoom In** menu item.
2. Select **Zoom In (+15%)**; verify the page content magnifies.
3. Select **Reset Zoom (100%)**; verify the page restores to standard 100% scale.

### Test Case 4: Developer Mode & Automated Isolation Test Suite
1. Go to **Settings > Developer Mode** and toggle it ON.
2. Tap **Open Developer Dashboard** and run **Run Isolation Test Suite**.
3. Verify all 4 tests pass with green badges.

---

## 7. Building the Application

To build the debug APK from the command line:

```bash
gradle :app:assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 8. Author Information

- **Developer**: Aminulsordar
- **Application**: Aminul MultiBook
- **Package**: `com.aminul.multibook`
- **Branding**: "Created by Aminulsordar"
