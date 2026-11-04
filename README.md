# App Ops

Android 15+ AppOps manager that runs through [Shizuku](https://shizuku.rikka.app/).

- Backend (Shizuku connection, AppOps read/write, history, templates, monitor, guard,
  native daemon) is taken from [AppOpsNext](https://github.com/1zumiii/AppOpsNext) 1.6.6.
- The UI shell (`presentation/AppOpsShell.kt`) is our own Material3 layout:
  working-mode picker, navigation drawer, options menu and the apps view.

The AppOpsNext code is licensed under GPL-3.0, so this repository is GPL-3.0 as well
(see `LICENSE`).

## Build

```
./gradlew :app:assembleDebug
```

Requires JDK 17 and Go (the ARM64 native daemon is compiled during the build).
CI: `.github/workflows/build.yml`.
