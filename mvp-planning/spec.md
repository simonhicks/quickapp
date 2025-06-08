# QuickApp DSL MVP Specification

## Overview

This document specifies the Minimal Viable Product (MVP) for a Kotlin-based DSL (“QuickApp”) that enables rapid prototyping of single-screen Android apps from a single `.kt` file. It covers:

1. Entrypoint & DSL discovery  
2. Manifest generation & permissions  
3. Gradle configuration & AndroidX dependencies  
4. CLI commands (`build` and `run`)  
5. DSL syntax and scopes (`app`, `screen`)  
6. UI primitives (`column`, `row`, `text`, `button`, `input`, `image`)  
7. Layout defaults and scrolling behavior  
8. Navigation and backstack behavior  
9. File I/O (internal storage)  
10. Networking (`httpGet`, `httpPost`)  
11. Logging and toasts  
12. Error handling conventions  
13. Assets and resource handling  
14. Deferred/future enhancements (not in MVP)

Each section includes type signatures (Kotlin), default values, and behavior details necessary for a developer to implement the runtime, compiler plugin (or script engine), and CLI tool.

---

## 1. Entrypoint & DSL Discovery

- **Single-File Limitation**  
  - Each QuickApp project is exactly one Kotlin script—e.g. `MyApp.kt`.  
  - No support for multiple source files or external imports in MVP.

- **`app(name: String, block: AppScope.() -> Unit)`**  
  - The user *must* begin their `.kt` with exactly one call to:
    ```kotlin
    app("My App Name") {
      // DSL contents here: zero or more screens
    }
    ```
  - If any `screen(...)` is declared outside `app { … }`, the build fails with a clear compiler-plugin error:  
    ```
    Error: screen() must be declared inside app("…") { … }
    ```
  - No top-level code other than `app(...)` is allowed. Any stray statements generate a compile-time error.

- **Runtime Behavior**  
  - At build time, a compiler plugin or script runner locates the `app(...)` invocation, captures its `name` argument, and begins registering the `AppScope`.  
  - Inside `AppScope`, each `screen("…") { … }` is registered in the application’s screen registry.  
  - The framework then generates (or uses a generated) `MainActivity` subclass that:
    1. Sets the app’s label to the `name` string.  
    2. Locks orientation to portrait.  
    3. On launch, inflates and shows the first-registered screen (in script order).  

---

## 2. Manifest Generation & Permissions

### 2.1 Package Name

- **Derived from Script Filename**  
  - Let the script file be named `FooBar.kt` (case-insensitive).  
  - Strip non-alphanumeric characters, convert to lowercase: e.g. `foo_bar.kt` → `foobar`.  
  - Construct package:  
    ```
    com.quickapp.generated.<cleaned_filename>
    ```
    e.g. `com.quickapp.generated.foobar`  
  - This ensures that two different scripts (with different filenames) install side-by-side without collision.

### 2.2 `<application>` Attributes

- **`android:label`**  
  - Taken directly from the `name` argument of `app(name) {…}`.  
  - Example:
    ```xml
    <application
        android:label="My App Name"
        android:icon="@mipmap/ic_launcher"
        …>
    ```
- **Default Icon**  
  - MVP does **not** support `app_icon(…)`. A default launcher icon (bundled in the runtime) is used.  
  - Launcher icon placed in standard `mipmap-*/` folders under `res/`.  
- **`android:screenOrientation="portrait"`**  
  - Locks the main activity to portrait to avoid handling rotation/state persistence.

### 2.3 Permissions

- **`<uses-permission android:name="android.permission.INTERNET" />`**  
  - Always included because networking (HTTP) is available.  
- **No storage permissions in MVP**  
  - File I/O (`readFile`/`writeFile`) targets *internal* app storage (`context.filesDir`), which requires no explicit permission.

### 2.4 Full Generated Manifest (MVP)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.quickapp.generated.<cleaned_filename>">

    <!-- INTERNET permission for HTTP calls -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:label="<App Name from DSL>"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="true"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

        </activity>
    </application>

</manifest>
```

- `<cleaned_filename>` and `<App Name from DSL>` are substituted at build time.  
- No other permissions or features are declared.

---

## 3. Gradle Configuration & AndroidX Dependencies

The CLI will generate a minimal `build.gradle.kts` for the MVP:

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quickapp.generated.<cleaned_filename>"
        minSdk     = 23
        targetSdk  = 34

        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // No additional dependencies in MVP
}
```

### 3.1 Rationale

- **`compileSdk = 34`**  
  - Build against the latest stable API (as of mid-2025) to opt into newest platform features/behavior.  
- **`minSdk = 23`**  
  - Supports Android 6.0+ devices (~>95% of active devices).  
  - Simplifies permissions (runtime permissions introduced in API 23).  
- **`targetSdk = 34`**  
  - Opts into the newest platform behavior (e.g. stricter background limits, file-access policies).  
  - Keeps the app compliant with modern expectations.  
- **Dependencies**  
  - **`core-ktx:1.11.0`** provides Kotlin extension functions for Android core APIs (e.g., `Context.filesDir`, `toast(...)`).  
  - **`appcompat:1.6.1`** enables `AppCompatActivity`, Material-style widgets, and backward compatibility down to API 23.  
  - No `ConstraintLayout`, no `lifecycle-runtime`, and no external networking library (use built-in `HttpURLConnection`).

---

## 4. CLI Commands (`quickapp`)

### 4.1 Prerequisites

- **Android SDK (platform tools + build tools)** installed, with `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variable pointing to SDK root.  
- **Kotlin 1.8+** (bundled or installed separately)—used to compile the DSL script.  
- **Gradle Wrapper** or bundled Gradle version in CLI (MVP uses a generated `build.gradle.kts` and either a wrapper or system Gradle).

### 4.2 `quickapp build <script.kt>`

- **Behavior**  
  1. Validate exactly one top-level `app(…) { … }` call.  
     - If missing or multiple, fail with:  
       ```
       Error: Exactly one top-level app(name) { … } block is required.
       ```
  2. Generate:
     - `MainActivity.kt` with boilerplate that:
       - Extends `AppCompatActivity`
       - Reads the DSL AST (registered screens) and inflates the first screen on `onCreate`
       - Implements backstack navigation and `goTo(...)`.
     - `AndroidManifest.xml` (as in Section 2).  
     - `build.gradle.kts` (as in Section 3).  
     - `settings.gradle.kts` with a single module.  
     - Copy the user’s `script.kt` into `src/main/java/com/quickapp/generated/`.  
     - If needed, generate any glue classes (e.g. for DSL runtime).
  3. Run Gradle with:
     ```
     ./gradlew assembleDebug
     ```
  4. On success, copy the resulting APK from `app/build/outputs/apk/debug/app-debug.apk` to:
     ```
     ./<cleaned_filename>-debug.apk
     ```
- **Output**  
  - `my_app-debug.apk` for `quickapp build my_app.kt`, placed in the current working directory.  
  - If build errors occur, bubble up the Kotlin or Gradle messages, prefixed with “QuickApp Build Error: …”.

### 4.3 `quickapp run <script.kt>`

- **Behavior**  
  1. If `<cleaned_filename>-debug.apk` does not exist or is older than `script.kt`, invoke `quickapp build <script.kt>`.  
  2. Query connected devices/emulators via `adb devices`.  
     - If exactly one device is connected → proceed.  
     - If zero or more than one → print:
       ```
       Error: exactly one connected device or emulator is required. Found: <N>.
       ```
       and exit with non-zero status.  
  3. Install the APK:
     ```
     adb install -r <cleaned_filename>-debug.apk
     ```
  4. Launch the main activity:
     ```
     adb shell am start -n com.quickapp.generated.<cleaned_filename>/.MainActivity
     ```
- **Output**  
  - Standard output logs from `adb install` and `adb shell am start`.  
  - Any `adb`-related errors (no device, installation failure) are returned to the user verbatim.

---

## 5. DSL Syntax and Scopes

### 5.1 Top-Level Structure

```kotlin
app(name: String) {
    // zero or more screens
    screen("main") { … }
    screen("settings") { … }
    // no code outside app { … }
}
```

- **`app(name, block)`**  
  - Entry point of the DSL.  
  - `name` → used for `android:label` and default `log` tag.  
  - `block: AppScope.() -> Unit` provides the context for `app`-level primitives (currently only `screen` in MVP).

- **Inside `AppScope`**  
  ```kotlin
  fun screen(name: String, block: ScreenScope.() -> Unit)
  ```
  - Registers a screen with ID = `name` (case-sensitive).  
  - The first-registered screen becomes the “home” screen (shown on app launch).  
  - All screen names must be unique; duplicate names cause a compile-time error:  
    ```
    Error: Duplicate screen name: "<name>"
    ```

### 5.2 `ScreenScope` Context

- Inside each `screen(name) { … }` block, the user may declare:
  - **Layout primitives**: `column { … }`, `row { … }`  
  - **UI elements**: `text(...)`, `button(...) { … }`, `input(...)`, `image(...)`  
  - **Navigation/helper calls**:  
    - `goTo("otherScreen")`  
    - `get("inputId")`   (returns `String?`)  
    - `readFile("path")`, `writeFile("path", content)`  
    - `httpGet(url) { … }`, `httpPost(url, formData) { … }`  
    - `log("message")`  
    - `toast("message")`  

- Any attempt to call these primitives outside `screen {…}` (or routing r.e. `goTo`) generates a compile-time error:  
  ```
  Error: <primitive>() must be called within a screen() block.
  ```

---

## 6. UI Primitives

### 6.1 Layout Containers

#### 6.1.1 `column(...) { … }`

```kotlin
fun column(
    padding: Int           = 16,              // dp
    spacing: Int           = 8,               // dp between children
    horizontalAlignment: Alignment = Alignment.Start,
    verticalAlignment:   Alignment = Alignment.Top,
    block: ColumnScope.() -> Unit
)
```

- **Behavior**  
  1. Renders as a `ScrollView` containing a vertical `LinearLayout`.  
  2. Adds `padding` dp on all four sides of the `LinearLayout`.  
  3. Places each child view in order, with `spacing` dp between them.  
  4. Aligns children horizontally according to `horizontalAlignment` (Start, Center, End) and vertically at `Top` (content flows downward).  
  5. Scrollbars appear only if total child height > viewport height.

- **Nested Layouts**  
  - If a `column` is nested inside another `column` or `row`, only the **outermost** `column` in that branch wraps in a `ScrollView`. Nested `column` simply become a plain `LinearLayout` (vertical) with the same padding/spacing/alignment.

#### 6.1.2 `row(...) { … }`

```kotlin
fun row(
    padding: Int           = 16,              // dp
    spacing: Int           = 8,               // dp between children
    horizontalAlignment: Alignment = Alignment.Start,
    verticalAlignment:   Alignment = Alignment.Top,
    block: RowScope.() -> Unit
)
```

- **Behavior**  
  1. Renders as a horizontal `HorizontalScrollView` containing a horizontal `LinearLayout`.  
  2. Adds `padding` dp on all four sides of the `LinearLayout`.  
  3. Places each child view in order, with `spacing` dp between them.  
  4. Aligns children vertically according to `verticalAlignment` (Top, Center, Bottom) and horizontally at `Start` (content flows left to right).  
  5. Scrollbars appear only if total child width > viewport width.

- **Nested Layouts**  
  - If a `row` is nested inside another `row` or `column`, only the **outermost** `row` in that branch wraps in a `HorizontalScrollView`. Nested `row` becomes a plain `LinearLayout` (horizontal).

### 6.2 UI Elements

#### 6.2.1 `text(...)`

```kotlin
enum class TextWeight { Normal, Bold }
enum class TextStyle  { Normal, Italic }
enum class TextAlign  { Start, Center, End }

fun text(
    content:   String,
    size:      Int            = 14,             // sp
    color:     String         = "#000000",      // hex (#RRGGBB)
    weight:    TextWeight     = TextWeight.Normal,
    style:     TextStyle      = TextStyle.Normal,
    textAlign: TextAlign      = TextAlign.Start
)
```

- **Units**  
  - `size` is in **sp** so it automatically scales with user font-size preferences.  
- **Style Combination**  
  - `weight = Bold` + `style = Italic` = bold-italic.  
- **Default Behavior**  
  - `text("…")` → 14sp, black, normal weight, normal style, left-aligned.

#### 6.2.2 `button(...) { … }`

```kotlin
fun button(
    label:     String,
    width:     Int?          = null,           // dp; null → “match parent” (minus padding) or “wrap content” if parent not stretching
    height:    Int           = 48,              // dp
    color:     String        = "#6200EE",       // background hex
    textColor: String        = "#FFFFFF",       // label hex
    weight:    TextWeight    = TextWeight.Normal,
    style:     TextStyle     = TextStyle.Normal,
    onClick:   () -> Unit
)
```

- **Defaults**  
  - Height = 48 dp (standard Material touch target).  
  - Width = `null`: by default, the button expands to fill its parent’s width minus parent padding. If parent is a `column` or `row` with no width constraint, it “wraps” the text.  
  - `color` = purple `#6200EE`, `textColor` = white.  
  - `weight/style` apply to the label.  
- **Behavior**  
  - Invokes `onClick` on the **main/UI thread**.  
  - Renders as a Material‐styled `Button`.  

#### 6.2.3 `input(...)`

```kotlin
fun input(
    id:   String,
    hint: String = ""
): Unit
```

- **Behavior**  
  1. Renders as a single‐line `EditText` with placeholder text = `hint`.  
  2. The `id` must be unique within the screen.  
  3. Developers retrieve its content via:
     ```kotlin
     val textValue: String? = get("inputId")
     ```
     - Returns `null` if no text entered or if `id` not found.  
- **No Additional Parameters in MVP**  
  - No `inputType` (number, email, password).  
  - No `maxLength`, no `multiline`. Those will be future enhancements.

#### 6.2.4 `image(...)`

```kotlin
fun image(
    path: String
): Unit
```

- **Behavior**  
  1. Loads the PNG/JPEG/WEBP from the `assets/` folder at the given `path` (relative to project root).  
  2. Renders at the image’s **intrinsic pixel dimensions** (no scaling or resizing).  
  3. If the image file is missing or fails to decode, throws a runtime exception (crashes the app) with a clear stack trace.  
- **No Optional Size/Scale Parameters in MVP**  
  - No `width`/`height` or scaling modes. Future releases may add `fitCenter`, `centerCrop`, etc.

---

## 7. Layout Defaults and Scrolling Behavior

1. **Padding & Spacing**  
   - Both `column` and `row` use:
     - `padding = 16.dp` on all sides.  
     - `spacing = 8.dp` between adjacent child views.  
   - Children are laid out inside a `LinearLayout` (vertical or horizontal) with those margins.

2. **Scrolling**  
   - **Top-level** `column` or `row` under each `screen` wraps in a scroll container (`ScrollView` or `HorizontalScrollView`) so content can scroll if it exceeds the viewport.  
   - **Nested** `column` or `row` inside another layout does **not** add a second scroll container; it is rendered as a plain `LinearLayout` with the same padding/spacing rules.

3. **Alignment**  
   - For `column`:  
     - `horizontalAlignment = Start` (left‐aligned).  
     - `verticalAlignment = Top` (content flows from top).  
     - Override if needed:  
       ```kotlin
       column(horizontalAlignment = Alignment.Center) { … }
       ```
   - For `row`:  
     - `horizontalAlignment = Start` (content flows from left).  
     - `verticalAlignment   = Top` (top‐aligned).  

4. **Units**  
   - `padding` and `spacing` are interpreted as **dp** (density-independent pixels). Implementation must convert to actual pixels at runtime using platform APIs.

---

## 8. Navigation and Backstack Behavior

- **`goTo(screenName: String)`**  
  ```kotlin
  fun goTo(screenName: String)
  ```
  - If `screenName` is registered (exact, case-sensitive), push the current screen onto an internal backstack and inflate the target screen’s view hierarchy.  
  - If not registered → immediately call `toast("Screen '<screenName>' not found")` (short duration) and do nothing else.  
  - Always run on the UI thread.

- **System Back Button**  
  - Pops the previous screen from the backstack (if any) and shows it.  
  - If the backstack is empty (user is on the “home” screen), the system Back action exits the app.

- **Initial Screen**  
  - The first `screen("…") { … }` defined inside `app(…)` is the home screen. On `MainActivity.onCreate`, this screen is inflated first.

---

## 9. File I/O (Internal Storage)

### 9.1 `readFile(path: String): String?`

```kotlin
fun readFile(path: String): String?
```

- **Behavior**  
  1. Resolves `path` relative to the app’s internal storage directory (`context.filesDir`).  
  2. If the file at `<filesDir>/<path>` exists, reads it as UTF-8 text and returns the `String`.  
  3. If the file does not exist or cannot be read, returns `null`.  
  4. If an unexpected I/O exception occurs, the exception bubbles up and crashes the app.

### 9.2 `writeFile(path: String, content: String)`

```kotlin
fun writeFile(path: String, content: String)
```

- **Behavior**  
  1. Resolves `path` relative to `context.filesDir`.  
  2. Ensures that any missing parent directories under `filesDir` are created.  
  3. Writes `content` as UTF-8 text, overwriting any existing file.  
  4. If an unexpected I/O exception occurs, it bubbles up and crashes the app.

### 9.3 No Permissions Required

- Since all file operations target **internal** storage (`filesDir`), no `WRITE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE` permissions are declared.

---

## 10. Networking (`httpGet`, `httpPost`)

### 10.1 `httpGet`

```kotlin
/**
 * Performs an HTTP GET request on a background thread.
 *
 * @param url      Fully qualified URL (e.g., "https://api.example.com/data").
 * @param callback Called on the main/UI thread with the response body as a String, or null on any error.
 */
fun httpGet(
    url: String,
    callback: (response: String?) -> Unit
)
```

- **Behavior**  
  1. On invocation, spawn a background thread (e.g., via a minimal Kotlin coroutine or `Thread`).  
  2. Use `java.net.HttpURLConnection` to open a connection to `url`.  
     - `requestMethod = "GET"`.  
     - Set reasonable timeouts (e.g., `connectTimeout = 15000`, `readTimeout = 15000`).  
  3. If response code is in 200–299: read `inputStream` into a `String` (UTF-8) and store it as `body`.  
     - Close streams and disconnect.  
     - Post `callback(body)` back to the main/UI thread.  
  4. If any exception (IOException, MalformedURLException) or non-2xx response:  
     - Post `callback(null)` on the main/UI thread.  
     - Swallow the exception after logging to Logcat.

### 10.2 `httpPost`

```kotlin
/**
 * Performs an HTTP POST request with form-encoded data on a background thread.
 *
 * @param url       Fully qualified URL.
 * @param formData  Map of key/value pairs to send as "application/x-www-form-urlencoded".
 * @param callback  Called on the main/UI thread with the response body as a String, or null on any error.
 */
fun httpPost(
    url: String,
    formData: Map<String, String>,
    callback: (response: String?) -> Unit
)
```

- **Behavior**  
  1. On invocation, spawn a background thread.  
  2. Build a form-encoded body string:
     ```kotlin
     val bodyString = formData.entries.joinToString("&") { (key, value) ->
         "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
     }
     ```
  3. Use `HttpURLConnection` to open a connection to `url`:
     - `requestMethod = "POST"`  
     - `doOutput = true`  
     - `setRequestProperty("Content-Type", "application/x-www-form-urlencoded")`  
     - Write `bodyString.toByteArray(Charsets.UTF_8)` to `outputStream`.  
     - Flush and close output stream.  
  4. If response code 200–299: read response body, post `callback(body)` on main thread.  
  5. On exception or non-2xx: post `callback(null)` on main thread.

---

## 11. Logging and Toasts

### 11.1 `log(message: String)`

```kotlin
fun log(message: String)
```

- **Behavior**  
  1. Uses Android’s `Log.i(tag, message)` (or `Log.d`).  
  2. `tag` is set to the `name` from `app(name)`. Example:  
     ```kotlin
     // If app("My App") { … }, then:
     Log.i("My App", "Something happened")
     ```
  3. No overload for custom tags in MVP.  
  4. Always prints to Logcat.

### 11.2 `toast(message: String)`

```kotlin
fun toast(message: String)
```

- **Behavior**  
  1. Always uses `Toast.LENGTH_SHORT`.  
  2. Invokes on the main/UI thread:  
     ```kotlin
     Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
     ```
  3. Can be called anywhere inside `screen { … }` or inside callbacks (e.g., `button { … }`, `httpGet` callback).  
  4. If called off the main thread, the framework posts to the UI thread internally.

---

## 12. Error Handling Conventions

- **Compile-time/DSL Errors**  
  - Missing or multiple `app(…) { … }` → error.  
  - `screen(…)` outside `app { … }` → error.  
  - Duplicate screen names → error.  
  - Unknown primitive invoked → error (e.g. calling `column` outside `screen`).  
- **Runtime Uncaught Exceptions**  
  - Any uncaught exception (e.g. NPE in user code, I/O error on write, JSON parsing if added later) → crash the app and print a stack trace in Logcat. MVP does **not** catch these or show user-friendly toasts.  
- **Domain-specific Errors**  
  - `goTo("nonexistent")` → short toast `"Screen 'nonexistent' not found"`, no crash.  
  - `readFile("missing.txt")` → returns `null`; developer must check for `null`.  
  - `httpGet`/`httpPost` failures → callback with `null`; developer must check for `null`.  
  - `image("missing.png")` → throws an exception (crashes the app) with a stack trace.  

---

## 13. Assets & Resource Handling

- **Assets Folder Layout (MVP)**  
  - The user’s script folder may contain an `assets/` directory at the same level as the `.kt` file.  
    ```
    MyApp.kt
    assets/
      logo.png
      data.json
    ```
  - When building:  
    1. CLI copies the entire `assets/` folder into `app/src/main/assets/` of the generated project.  
    2. `image("assets/logo.png")` loads from that folder at runtime.  

- **Density Buckets**  
  - MVP does **not** support density-specific assets (e.g. `drawable-hdpi/`, `drawable-xhdpi/`). All images live under a single `assets/` folder and are rendered at intrinsic pixel size.  
  - Future releases may add support for DPI folders or scaling options.

- **Fonts & Audio (Deferred)**  
  - MVP does not include methods for loading custom fonts or playing audio. Those are out of scope.

---

## 14. Deferred/Future Enhancements (Out of Scope in MVP)

1. **`app_icon("…")`**  
   - Ability to specify a custom launcher icon in DSL. Deferred.

2. **`setup { … }`**  
   - Application-level initialization block outside screens. Deferred.

3. **Advanced Layout Options**  
   - Additional containers (e.g., `stack`, `constraintlayout`).  
   - More granular styling on containers (background color, margins, corner radius).  

4. **Expanded UI Primitives**  
   - `slider`, `switch`, `checkbox`, multi-line `input`, custom `inputType` (number/email/password).  
   - `image` scaling options (`fitCenter`, `centerCrop`) and explicit `width`/`height`.

5. **Permissions Detection**  
   - Dynamically analyze which permissions are needed (e.g., only include `INTERNET` if `httpGet` is used). Deferred in favor of always including `INTERNET` for MVP.

6. **Custom Package/Version Overrides**  
   - DSL primitives for `packageName`, `versionCode`, `versionName`. Deferred.

7. **Release Signing**  
   - Support `quickapp build --release --keystore ...`. Deferred.

8. **Automatic Emulator Launch**  
   - If no device is connected, CLI automatically spins up an emulator. Deferred.

9. **Orientation Support & State Preservation**  
   - Support landscape mode and preserve input state on rotation (MVP locks to portrait).

10. **Multi-File Projects & Imports**  
    - Allow multiple `.kt` source files and `import` statements. Deferred.

11. **Accessibility (a11y)**  
    - DSL support for `contentDescription` on `image`, `importantForAccessibility` flags, etc.

12. **Internationalization (i18n)**  
    - Support for string resources instead of hard-coded literals.

13. **Testing Infrastructure**  
    - Unit tests for DSL parser, golden tests for generated View hierarchies, instrumentation tests. Deferred.

---

## 15. Full Example (MVP)

Given a script `MyApp.kt`:

```kotlin
app("Shopping List") {
    screen("main") {
        column {
            text(
                "Shopping List",
                size = 20,
                weight = TextWeight.Bold,
                textAlign = TextAlign.Center
            )

            input(id = "itemInput", hint = "Enter item name")

            button("Add Item", width = null, height = 48) {
                val newItem = get("itemInput")
                if (newItem != null && newItem.isNotBlank()) {
                    log("Adding item: $newItem")
                    // Here we might write to file or update a list
                } else {
                    toast("Please enter an item")
                }
            }

            image("assets/logo.png")

            button("Go to Details") {
                goTo("details")
            }
        }
    }

    screen("details") {
        column {
            text("Details Screen", size = 18, weight = TextWeight.Bold)
            button("Back") {
                // system back button also works; this is optional
                goTo("main")
            }
        }
    }
}
```

- **Build**  
  ```
  quickapp build MyApp.kt
  ```
  → generates `myapp-debug.apk` in current directory.

- **Run**  
  ```
  quickapp run MyApp.kt
  ```
  → requires exactly one connected device, installs and launches `Shopping List` app, locked to portrait.

- **Manifest** (auto-generated)  
  ```xml
  <manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="com.quickapp.generated.myapp">

      <uses-permission android:name="android.permission.INTERNET" />

      <application
          android:label="Shopping List"
          android:icon="@mipmap/ic_launcher"
          android:allowBackup="true"
          android:supportsRtl="true">

          <activity
              android:name=".MainActivity"
              android:exported="true"
              android:screenOrientation="portrait">

              <intent-filter>
                  <action android:name="android.intent.action.MAIN" />
                  <category android:name="android.intent.category.LAUNCHER" />
              </intent-filter>

          </activity>
      </application>
  </manifest>
  ```

- **Generated Gradle Config**  
  ```kotlin
  plugins {
      id("com.android.application")
      kotlin("android")
  }

  android {
      compileSdk = 34

      defaultConfig {
          applicationId = "com.quickapp.generated.myapp"
          minSdk     = 23
          targetSdk  = 34

          versionCode = 1
          versionName = "1.0"
      }

      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_17
          targetCompatibility = JavaVersion.VERSION_17
      }
      kotlinOptions {
          jvmTarget = "17"
      }
  }

  dependencies {
      implementation("androidx.core:core-ktx:1.11.0")
      implementation("androidx.appcompat:appcompat:1.6.1")
  }
  ```

This full example demonstrates the combined DSL, manifest, Gradle config, and CLI workflow that make
up the MVP. Developers can use this spec to implement:

- A compiler plugin (or script runner) that transforms the DSL into Android code.  
- A runtime library that provides all primitives (`app`, `screen`, `column`, `text`, etc.).  
- A CLI that handles build and run, including package-name derivation and error-checking.

Once implemented, this will allow rapid prototyping of simple Android apps from a single `.kt`
script, fully encapsulating manifest generation, dependency management, layout, navigation, I/O,
networking, and logging—all without requiring developers to manually manage Android XML or Gradle.
