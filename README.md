# Android Helper

A Kotlin Compose Desktop app that wraps ADB, Maestro UI automation, and GitHub Actions into a single GUI for QA on DAZN, Kayo Sports, and Binge.
<img width="775" height="581" alt="image" src="https://github.com/user-attachments/assets/88b57b9e-cf93-4953-86fc-000b64f9205b" />

---

## Prerequisites

| Tool | Purpose | Install |
|---|---|---|
| JDK 17+ | Build + runtime | `brew install openjdk@17` |
| Android Platform Tools | ADB | `brew install --cask android-platform-tools` |
| Maestro CLI | UI automation flows | `curl -fsSL "https://get.maestro.mobile.dev" \| bash` |
| GitHub CLI | Workflow auth | `brew install gh` then `gh auth login` |
| scrcpy | Screen mirroring / recording | `brew install scrcpy` |

The app resolves each binary by probing known install locations (`ToolPaths.kt`), so they do **not** need to be on `PATH`. A GUI-launched `.app` inherits no shell environment, which is why probing is used instead of `which`.

---

## Setup

### 1. Create `local.properties`

In the project root, next to `build.gradle.kts`:

```properties
PROD_API_KEY=your_production_key_here
STAG_API_KEY=your_staging_key_here
```

This file is gitignored and must never be committed. Request the key values from the tool owner.

### 2. Generated `AppConfig.kt`

The `generateConfig` Gradle task reads `local.properties` and emits a source file into `build/generated/config`:

```kotlin
package config

object AppConfig {
    const val PROD_API_KEY: String = "..."
    const val STAG_API_KEY: String = "..."
}
```

You do not write or edit this file — it is generated on every build and the directory is registered as a source set. Missing keys default to empty strings, so the app still compiles but any API-backed feature (e.g. Clear Phone Number) will fail at runtime.

> **Note:** `config.AppConfig` (generated keys) is distinct from `utils.helpers.AppConfig` (resolved binary paths). Import the right one.

Force a regeneration after editing `local.properties`:

```bash
./gradlew generateConfig --rerun-tasks
```

### 3. Run from source

```bash
./gradlew run
```

---

## Building a distributable

### DMG

```bash
./gradlew packageDmg
```

Output: `build/compose/binaries/main/dmg/Android Helper-<version>.dmg`

Other formats: `packageDeb`, `packageMsi`, or `packageDistributionForCurrentOS`.

### Unpackaged `.app` (faster iteration)

```bash
./gradlew createDistributable
```

Output: `build/compose/binaries/main/app/Android Helper.app`

### Debugging a packaged build

Finder discards stdout. Launch the inner binary directly to see diagnostic output:

```bash
"build/compose/binaries/main/app/Android Helper.app/Contents/MacOS/Android Helper"
```

Useful because path resolution logs which flows directory and which binaries were found.

---

## Maestro flows

Flows must ship as **real files on disk**, not jar resources — Maestro is a CLI that takes a filesystem path. They therefore live under the Compose app-resources root, not `src/main/resources`:

```
resources/
└── common/
    └── maestroflows/
        ├── dazn/       login_valid.yaml, logout.yaml, create_account.yaml, ...
        ├── kayo/       login_valid_kayo.yaml, logout_kayo.yaml, ...
        ├── binge/      login_valid_binge.yaml, logout_binge.yaml, ...
        └── puredome/   puredomevpnmastertest.yaml
```

```kotlin
nativeDistributions {
    appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
}
```

Compose **strips the `common/` segment** when staging, so at runtime flows resolve to `<compose.application.resources.dir>/maestroflows/`. `MaestroRunner.FLOWS_ROOT` tries that first, then falls back to the source tree so `./gradlew run` works unchanged.

App icons stay in `src/main/resources/icon/` — `painterResource` reads from the classpath.

### Flow conventions

Front matter is parameterised so one flow serves both environments:

```yaml
appId: ${APP_ID}
name: KAYO Valid Login
tags: [smoke, login]
---
- launchApp
- runFlow: dismiss_popups_kayo.yaml
```

- `${APP_ID}` is injected as `-e APP_ID=<selected package>`
- `runFlow:` uses **bare filenames** — resolved relative to the flow's own directory
- Per-brand filename suffixes (`_kayo`, `_binge`) are applied by `MaestroApp.resolveFlowFile()`; DAZN files are unsuffixed

---

## Configuration

### Adding an app

Add to `appConfigs` in `CommandList.kt`:

```kotlin
"NEW_APP" to AppConfig(
    displayName = "New App",
    prodPackage = "com.example.newapp",
    stagPackage = "com.example.newapp.debug",
    activityPath = "com.example.newapp.MainActivity",
    iconRes = "icon/newapp.png"
)
```

Verify package names against a real device — a mismatch breaks both app detection and Maestro targeting:

```bash
adb shell pm list packages | grep -i newapp
```

For Maestro support, also add a `MaestroApp` enum entry and a matching flows directory. The enum `key` must match the `appConfigs` key.

### Adding a Maestro automation

1. Add the YAML to each brand folder
2. Add an `AutomationOption` to `automationOptions` in `CommandDropDown.kt`
3. Wire the branch in `automationDialog`'s `onOptionSelected`

`prodOnly = true` disables it on STAGING; `usesMaestro = true` locks it while another flow runs.

### CI/CD workflow inputs

Not configured in the app. Inputs are read live from `.github/workflows/apk-builder-mobile.yml` on the selected branch and parsed with SnakeYAML — adding an input to the workflow YAML is sufficient.

---

## Device setup

- **USB:** enable Developer Options → USB Debugging, accept the RSA prompt
- **Wireless (Android 11+):** Developer Options → Wireless Debugging → Pair with code, then `adb pair <ip:port>` and `adb connect <ip:port>`

### Maestro driver

Maestro deploys a driver app (`dev.mobile.maestro`) on first run. Verify:

```bash
adb -s <serial> shell pm list instrumentation | grep maestro
```

If empty, the automated install was blocked (common on Samsung devices with Auto Blocker enabled). Install manually:

```bash
cd /tmp && unzip -o ~/.maestro/lib/maestro-client.jar "*.apk"
adb -s <serial> install -r -t /tmp/maestro-app.apk
adb -s <serial> install -r -t /tmp/maestro-server.apk
```

On Samsung, also disable **Settings → Security and privacy → Auto Blocker** and reboot.

> Connect **one device at a time** when running Maestro flows. Maestro has a known session-mixing bug that can target the wrong device even with `--device` specified.

### VPN

The VPN feature drives the PureDome B2B app (`com.vpn.android.pureb2b`) via Maestro. It must be installed and logged in on the device. The app checks for it and shows a remediation dialog with a Re-check button if absent.

---

## Troubleshooting

| Symptom | Cause |
|---|---|
| `Flows directory not found` | Flows not under `resources/common/maestroflows`, or `appResourcesRootDir` not set |
| `Package ... is not installed` | `appConfigs` package name doesn't match the device |
| `StatusRuntimeException: UNAVAILABLE` | Maestro driver not installed or not running |
| `Unable to launch app undefined` | `${APP_ID}` not passed — no environment selected |
| Binary not found in packaged app but works via Gradle | Path not in `ToolPaths.kt` probe list |
| Empty `PROD_API_KEY` | `local.properties` missing or `generateConfig` not re-run |

---

## Project layout

```
├── resources/common/maestroflows/    # Maestro YAML (staged into bundle)
├── src/main/kotlin/
│   ├── commands/                     # appConfigs, command definitions, regions
│   ├── ui/components/                # Compose UI, dialogs, buttons
│   ├── utils/helpers/                # ADB, Maestro, GitHub clients, ToolPaths
│   └── views/                        # MainScreen
├── src/main/resources/icon/          # App icons (jar resources)
├── local.properties                  # API keys — gitignored
└── build.gradle.kts
```
