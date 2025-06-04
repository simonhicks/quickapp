# Shoes for Android – Project Blueprint & LLM Prompt Kit

Below is a **three‑pass plan** followed by an **LLM‑prompt kit**.  
Read straight through once; every section intentionally builds on the previous one.

---

## ① End‑to‑end blueprint (first pass – “big strokes”)

| Area | What has to exist | Key acceptance signals |
|------|------------------|-------------------------|
| **Gradle workspace** | Multi‑module build (`runtime`, `plugin`, `cli`, later `sample‑app`) using Kotlin 17 and AGP 8 | `./gradlew help` succeeds; dependency graph resolves |
| **Runtime library** | Pure‑Kotlin façade that exposes the DSL primitives (`app`, `screen`, `column`, `text`, …) and delegation stubs to Android‑specific implementations | Unit tests show that `AppScope` collects screens and prevents duplicates |
| **Compiler plugin / script runner** | Captures `app()` AST, derives package name from filename, emits generated sources (MainActivity, manifest, build files) | Compiling `Hello.kt` produces `app‑debug.apk` without manual Android XML |
| **CLI (`quickapp`)** | Rust or Kotlin/JVM executable with `build` and `run` sub‑commands mirroring spec §4 | `quickapp build Foo.kt` outputs `foo‑debug.apk`; `quickapp run Foo.kt` installs on exactly one device |
| **UI bridge** | Android implementation of layout & view primitives that respect defaults (16 dp padding, 8 dp spacing, scrolling rules) | Espresso golden test shows view hierarchy matches expectations |
| **Testing harness** | JUnit 5 + Kotest for JVM logic, Robolectric for UI, Gradle TestKit for CLI | CI passes on push; every failing DSL invocation returns a clear message as per spec |

---

## ② Chunking into deliverable iterations (second pass)

1. **M0 · Repo scaffold** – Gradle workspace & CI skeleton  
2. **M1 · Runtime core** – data classes + `AppScope`/`ScreenScope` registration  
3. **M2 · Plugin skeleton** – KSP/Kotlin Compiler Plugin that loads but does not yet generate code  
4. **M3 · Package‑name utility & tests** – pure function `cleanFilename("Foo.kt") → "foo"` + tests  
5. **M4 · AST capture** – plugin collects `app()`/`screen()` metadata into a model file  
6. **M5 · Code gen v1** – generate `MainActivity` + manifest with INTERNET permission  
7. **M6 · CLI skeleton** – `quickapp --help`, environment validation  
8. **M7 · CLI build** – wires M5 output into `./gradlew assembleDebug`  
9. **M8 · CLI run** – ADB device detection + install + launch  
10. **M9 · Layout primitives** – `column`, `row`, scrolling behaviour  
11. **M10 · UI widgets** – `text`, `button`, `input`, `image` backing views  
12. **M11 · Navigation runtime** – backstack + `goTo` + system back hook  
13. **M12 · Networking & I/O helpers** – `httpGet`, `httpPost`, `readFile`, `writeFile`  
14. **M13 · Polish & example app** – match the “Shopping List” demo, docs, release notes  

Each milestone yields a working, mergeable branch, plus green tests.

---

## ③ Right‑sizing (third pass – micro‑steps)

Below is **M0** expanded; apply the same technique to every milestone when writing prompts.

> **M0 micro‑steps**  
> 0.0 Create empty Git repository, set default branch `main`  
> 0.1 Add `.editorconfig`, `.gitignore` for Kotlin/Gradle/IntelliJ  
> 0.2 Generate `settings.gradle.kts` with root project name `quickapp`  
> 0.3 Add sub‑projects `runtime`, `plugin`, `cli` in `settings.gradle.kts`  
> 0.4 Add minimal `build.gradle.kts` in each module (no dependencies yet)  
> 0.5 Configure root `build.gradle.kts` with Kotlin JVM + Kapt plugins  
> 0.6 Add GitHub Actions workflow (JDK 17, cache Gradle) that runs `./gradlew build`  
> 0.7 Commit & push; CI passes  

Similar 6‑to‑8‑line task lists exist for every milestone; each one can be finished inside a small PR and covered by either unit or integration tests.

---

## ④ Prompt kit for a code‑generation LLM  

*(All prompts are fenced as **text**; copy‑paste each into your LLM in order.  
Each prompt assumes the repo state from the previous prompt has already been applied and tests are passing.)*

### Prompt 01 – Repo Scaffold
```text
You are working in an empty Git repo.  
Task: create a Gradle 8.8 multi‑module Kotlin workspace with sub‑projects `runtime`, `plugin`, and `cli`.  
Steps:
1. Write `settings.gradle.kts` that includes the three modules.
2. Add root `build.gradle.kts` configuring Kotlin JVM 1.9.23, Java 17 toolchain, and a `publish` task placeholder.
3. For each sub‑project, add a minimal `build.gradle.kts` applying the Kotlin plugin only.
4. Add `.gitignore` (IDEA, Gradle, macOS, Linux).
5. Commit all files.
No production code yet; `./gradlew help` must succeed.
Write a *single* patch set that achieves this.
```

### Prompt 02 – CI Bootstrap
```text
Extend the repo from Prompt 01.  
Add a GitHub Actions workflow `.github/workflows/ci.yml` that:
* runs on every push and PR,
* sets up JDK 17 and Gradle cache,
* executes `./gradlew build --no-daemon`.
Ensure build passes on empty project.  
Provide the new file content only.
```

### Prompt 03 – Runtime Data Model
```text
Create package `quickapp.runtime.model` in `runtime` module.

1. Add data class `ScreenDef(val name: String)`.
2. Add class `AppDef(val name: String) { val screens = mutableListOf<ScreenDef>() }`.
3. Add extension function `fun AppDef.screen(name: String, block: ScreenDef.() -> Unit = {}) { ... }` that:
   * throws `IllegalArgumentException` if `name` already exists.
4. Write JUnit5 test `AppDefTest` verifying:
   * adding duplicate screen names throws,
   * first registered screen is index 0 in `screens`.

Commit code and test.
```

### Prompt 04 – Filename Cleaning Utility
```text
Implement pure function `FilenameCleaner.kt` in `plugin` module:

```kotlin
fun cleanFilename(fileName: String): String
```

Rules (from spec):
* strip non‑alphanumeric characters,
* lowercase the result,
* strip file extension.

Add JUnit test cases:
* `"FooBar.kt" → "foobar"`
* `"foo_bar.kt" → "foobar"`
Place tests in `plugin/src/test` and update `build.gradle.kts` to use JUnit5.

Ensure `./gradlew :plugin:test` passes.
```

### Prompt 05 – Plugin Skeleton
```text
Add Kotlin Compiler Plugin (KCP) baseline in `plugin` module:

* Use the `org.jetbrains.kotlin.compiler.plugin` Gradle plugin.
* Provide entry class `QuickAppComponentRegistrar` that currently just logs "QuickApp plugin loaded".
* Configure `build.gradle.kts` so that compiling any module on the command line applies the plugin via `-Xplugin`.
* Add integration test using Gradle TestKit: compiling an empty Kotlin file with the plugin should succeed and log the message.

Push passing tests.
```

### Prompt 06 – AST Capture
```text
Enhance `QuickAppComponentRegistrar`:

1. Whenever it sees a top-level `app()` call, collect its string literal parameter.
2. Navigate the nested `screen()` calls and store names in a JSON file `META-INF/quickapp/app-model.json` within the compilation output.
3. If zero or multiple top-level `app()` calls exist, compilation should fail with the messages from the spec.

Add unit test compiling a sample DSL script with two screens and assert the JSON file content via the Kotlin compilation output API.
```

### Prompt 07 – Code Generation v1
```text
Using the model from Prompt 06, generate sources into `build/generated/quickapp/`:

* `MainActivity.kt` extending `AppCompatActivity` that inflates the first screen.
* `AndroidManifest.xml` exactly matching the MVP rules (portrait, INTERNET permission).

Add a Gradle functional test that builds an `example.kt` script and asserts that the resulting `manifest` file contains `<uses-permission android:name="android.permission.INTERNET"/>`.
```

### Prompt 08 – CLI Skeleton
```text
Create a `cli` module executable via `./gradlew :cli:installDist`.

* Use Kotlin x-cli (or picocli) for argument parsing.
* Sub-commands: `build` and `run` (no logic yet, just prints "stub").

Add unit test that `quickapp --help` exits with code 0.
```

### Prompt 09 – CLI Build Implementation
```text
Inside `cli`:

1. For `build <script>`:
   * Run the Kotlin compiler with the plugin to generate an Android project in `build/out/<cleanedFilename>`.
   * Invoke `./gradlew assembleDebug` inside that project.
   * Copy the resulting APK to `<cwd>/<cleanedFilename>-debug.apk`.

2. Emit errors exactly as in the spec when validation fails.

Add integration test using TestKit + ADB mock (injectable) that verifies the APK file is produced.
```

### Prompt 10 – CLI Run Implementation
```text
Extend Prompt 09:

* Detect connected devices via `adb devices` (allow injecting a fake `adb` in tests).
* Fail when device count ≠ 1.
* Install and launch the APK as per spec.

Add test covering the "zero devices" error path.
```

### Prompt 11 – Layout Primitives
```text
In `runtime`:

* Add `ColumnScope`, `RowScope`, and their builder functions with default padding/spacing constants.
* Implement Android backing classes in an `androidRuntime` source set using `LinearLayout` wrapped by `ScrollView` when top-level.
* Add Robolectric test verifying that a top-level column is wrapped in `ScrollView`, nested one is not.

Update dependent modules and CI.
```

### Prompt 12 – UI Widgets
```text
Add implementations for:
* `text()` → `TextView`
* `button()` → `MaterialButton`
* `input()` → `EditText`
* `image()` → `ImageView` loading from assets via `AssetManager.open()`

Provide Robolectric tests asserting default styles (size, colors, etc.).
```

### Prompt 13 – Navigation Runtime
```text
Implement `Navigator` singleton inside runtime:

* maintains `backStack: ArrayDeque<View>`
* `goTo(screenName)` pushes current view, inflates target, or shows toast if missing.
* System back overrides `onBackPressed` in generated `MainActivity`.

Write instrumentation test (Robolectric) ensuring backStack pops correctly.
```

### Prompt 14 – Networking & I/O
```text
Add `httpGet`, `httpPost`, `readFile`, `writeFile` helpers.

* Use `HttpURLConnection` with timeouts.
* Post callbacks on main thread via `Handler(Looper.getMainLooper())`.

Add unit tests with MockWebServer (OkHttp) for 200 and 500 responses.
```

### Prompt 15 – End‑to‑end Example
```text
Create `sample/ShoppingList.kt` matching the spec example.

* `quickapp build sample/ShoppingList.kt` must succeed.
* Run instrumentation test that launches the APK and finds `Shopping List` title in the view hierarchy.

Update README with usage instructions.
```

---

### How to use the kit

Run each prompt sequentially with your code‑generation LLM.  
After every prompt, run the tests locally; they should all stay green.  
The final prompt wires everything together—no dangling code, fully runnable MVP.

Happy building!
