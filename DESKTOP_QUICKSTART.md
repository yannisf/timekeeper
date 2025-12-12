# Timekeeper Desktop - Quick Start Guide

## What Was Built

A macOS system tray application that integrates with the Timekeeper CLI. The app runs in the menu bar and provides:
- **Click menu** with Tick, Status, and Exit options
- **Status dialog** showing daily work summary
- **Notifications** for tick operations
- **Seamless integration** with your existing CLI workflow

## Before You Start

On **macOS**, ensure you have:

1. **Java 21+**
   ```bash
   java -version
   ```

2. **`timekeeper` binary** installed and in PATH
   ```bash
   which timekeeper
   ```

   If not installed, build and install it:
   ```bash
   ./gradlew nativeCompile
   cp app/build/native/nativeCompile/timekeeper /usr/local/bin/
   ```

## Build the App (on macOS)

**One command:**
```bash
./build-macos-app.sh
```

This creates:
- `Timekeeper.app` - The application bundle
- `Timekeeper-1.0.0.dmg` - Distributable installer

**Manual build:**
```bash
./gradlew :desktop:packageDmg
```

Output: `desktop/build/compose/binaries/main/dmg/Timekeeper-1.0.0.dmg`

## Install the App

1. Open the DMG file: `Timekeeper-1.0.0.dmg`
2. Drag `Timekeeper.app` to Applications folder
3. Or run: `open /Applications/Timekeeper.app`

## Use the App

The app appears in your menu bar (top-right). Click the icon to see the menu:

- **Tick** - Start/stop work tracking
  - Shows success notification
  - Executes `timekeeper tick`

- **Status** - View daily summary
  - Shows in scrollable dialog
  - Displays work duration, breaks, etc.

- **Exit** - Quit the app

## Troubleshooting

| Issue | Solution |
|-------|----------|
| App won't start | Ensure `timekeeper` is in PATH: `which timekeeper` |
| Menu doesn't appear | Restart the app, check menu bar (top-right) |
| Status shows blank | Run `timekeeper status` manually to test CLI |
| "App is damaged" | `xattr -d com.apple.quarantine /Applications/Timekeeper.app` |

## Database

The app shares the same database as the CLI:
- Default location: `~/.timekeeper.db`
- Override: `export TIMEKEEPER_DB_PATH=/custom/path` before launching

You can use both the CLI and desktop app interchangeably - they share the same data!

## For Distribution

To share the app with others:

1. **Code Sign** (recommended):
   ```bash
   codesign --force --deep --sign "Developer ID Application: Your Name" \
       "Timekeeper.app"
   ```

2. **Notarize** (required for untrusted installations):
   ```bash
   xcrun notarytool submit "Timekeeper-1.0.0.dmg" \
       --apple-id "your@email.com" \
       --team-id "XXXXXXXXXX" \
       --password "app-specific-password" \
       --wait
   ```

See `DESKTOP_BUILD.md` for detailed distribution steps.

## Need Help?

- 📖 Full docs: `DESKTOP_BUILD.md`
- 🔧 Architecture: `/home/yannis/.claude/plans/shimmering-mixing-cookie.md`
- 🐛 Debug: Run `./gradlew :desktop:build --stacktrace`
