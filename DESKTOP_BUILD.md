# Timekeeper Desktop App - Build Instructions

This document provides instructions for building the Compose Multiplatform Desktop system tray app for macOS.

## Prerequisites

### On macOS (for creating the app bundle and DMG)

1. **Java 21 or later** (for building)
   ```bash
   java -version
   ```

2. **Gradle 8.8+** (included in the project via `gradlew`)

3. **Installed `timekeeper` CLI binary** in your PATH

   Build and install from the app module:
   ```bash
   # First, build the native CLI binary
   export GRAALVM_HOME=/path/to/graalvm  # If using GraalVM native compilation
   ./gradlew -Pagent run --args="start"  # Agent mode to generate reflection configs
   ./gradlew nativeCompile

   # Copy to a location in your PATH
   cp app/build/native/nativeCompile/timekeeper /usr/local/bin/

   # Verify it's working
   timekeeper --help
   ```

4. **macOS 10.13 or later**

## Quick Start

### One-Command Build

Run the provided build script (on macOS):

```bash
./build-macos-app.sh
```

This will:
1. Build the desktop module
2. Create the macOS app bundle (.app)
3. Create the distributable DMG file
4. Display the output location

### Manual Build Steps

#### Step 1: Build the Desktop Module

```bash
./gradlew :desktop:build
```

#### Step 2: Create the macOS App Bundle and DMG

```bash
./gradlew :desktop:packageDmg
```

The DMG file will be created at:
```
desktop/build/compose/binaries/main/dmg/Timekeeper-1.0.0.dmg
```

## Output Files

After successful build, you'll have:

```
desktop/build/compose/binaries/main/
├── app/
│   └── Timekeeper.app/           # The macOS application bundle
└── dmg/
    └── Timekeeper-1.0.0.dmg      # Distributable DMG file
```

### App Bundle Contents

```
Timekeeper.app/
├── Contents/
│   ├── Info.plist                 # macOS metadata (LSUIElement=true for menu bar)
│   ├── MacOS/
│   │   └── Timekeeper             # Native launcher binary
│   ├── Resources/
│   │   └── app-icon.icns          # App icon
│   └── runtime/                   # Bundled Java Runtime
```

## Using the App

### Installation

1. Download and open the DMG file
2. Drag `Timekeeper.app` to the Applications folder
3. Run: `open /Applications/Timekeeper.app` or use Spotlight

### First Run

The app will:
- Run in the menu bar (top-right system tray area)
- Show no dock icon
- Provide a menu with three options:
  - **Tick** - Execute work tracking tick
  - **Status** - Show daily summary in a dialog
  - **Exit** - Quit the application

### Database Location

The app shares the same database as the CLI:
- Default: `~/.timekeeper.db`
- Override: Set `TIMEKEEPER_DB_PATH` environment variable

```bash
# Example: Use custom database location
export TIMEKEEPER_DB_PATH=~/.local/share/timekeeper.db
open /Applications/Timekeeper.app
```

## Building for Distribution

### Code Signing (Optional but Recommended)

To sign your app with a Developer ID certificate:

```bash
codesign --force --deep --sign "Developer ID Application: Your Name" \
    "desktop/build/compose/binaries/main/app/Timekeeper.app"
```

### Notarization (Required for Distribution)

For users running macOS 10.15+, Apple requires notarization:

```bash
# Submit for notarization
xcrun notarytool submit "desktop/build/compose/binaries/main/dmg/Timekeeper-1.0.0.dmg" \
    --apple-id "your-apple-id@example.com" \
    --team-id "XXXXXXXXXX" \
    --password "your-app-specific-password" \
    --wait

# Staple the ticket to your app
xcrun stapler staple "desktop/build/compose/binaries/main/app/Timekeeper.app"
```

#### Notarization Steps Explained

1. **Create an App-Specific Password**
   - Go to https://appleid.apple.com/account/manage
   - Security → App-Specific Passwords → Generate password

2. **Find Your Team ID**
   - Apple Developer Account → Membership
   - Your Team ID is shown in the Membership section

3. **Staple the Notarization Ticket**
   - After successful notarization, staple the ticket to your app
   - Users can then run the app without warnings

## Development/Testing

### Run During Development

```bash
# Ensure timekeeper binary is in PATH
./gradlew :desktop:run
```

This will launch the app directly from the development build.

### Rebuilding

If you make changes to the code:

```bash
# Full rebuild
./gradlew :desktop:clean :desktop:build

# Or with packaging
./gradlew :desktop:clean :desktop:packageDmg
```

## Troubleshooting

### App won't launch

**Problem**: App starts but immediately closes
- **Solution**: Check that `timekeeper` binary is in PATH
  ```bash
  which timekeeper
  ```

**Problem**: "App is damaged" message
- **Solution**: The app needs code signing
  ```bash
  xattr -d com.apple.quarantine /Applications/Timekeeper.app
  codesign --force --deep --sign - /Applications/Timekeeper.app
  ```

### Can't find `timekeeper` command

**Problem**: "timekeeper: command not found"
- **Solution**: Ensure the native binary is installed and in PATH
  ```bash
  # Build and install
  ./gradlew nativeCompile
  cp app/build/native/nativeCompile/timekeeper /usr/local/bin/
  timekeeper status  # Verify it works
  ```

### Menu not appearing on click

**Problem**: Clicking tray icon doesn't show menu
- **Solution**:
  1. Check system tray area (top-right on menu bar)
  2. Verify app is running: `ps aux | grep Timekeeper`
  3. Restart the app

### Status dialog shows blank

**Problem**: Status window shows but text is empty
- **Solution**:
  1. Verify `timekeeper` binary is working:
     ```bash
     timekeeper status
     ```
  2. Check database location:
     ```bash
     ls -la ~/.timekeeper.db
     ```

## Project Structure

```
timekeeper/
├── app/                           # CLI application (unchanged)
├── desktop/                       # Desktop UI module (new)
│   ├── src/main/
│   │   ├── kotlin/eu/frlab/timekeeper/desktop/
│   │   │   ├── DesktopApp.kt       # Entry point
│   │   │   ├── CliExecutor.kt      # Execute timekeeper binary
│   │   │   ├── TrayManager.kt      # System tray integration
│   │   │   └── StatusWindow.kt     # Status dialog UI
│   │   └── resources/icons/        # Icons
│   └── build.gradle.kts           # Compose Desktop configuration
├── gradle/libs.versions.toml      # Dependency versions (updated)
├── settings.gradle.kts            # Module includes (updated)
└── build-macos-app.sh             # One-command build script
```

## Technical Details

- **Framework**: Compose Multiplatform Desktop
- **Language**: Kotlin 2.2.21
- **Java Target**: Java 21
- **System Tray**: Java AWT (SystemTray API)
- **Async**: Kotlin Coroutines
- **Bundled JRE**: Included in app bundle

## Building on Other Platforms

This desktop module is designed for macOS. To build for other platforms:

- **Linux**: Use `./gradlew :desktop:packageDeb` (creates .deb package)
- **Windows**: Use `./gradlew :desktop:packageMsi` (creates .msi installer)

Build tasks will only work on the target platform.

## Support

If you encounter issues:

1. Check the plan file: `/home/yannis/.claude/plans/shimmering-mixing-cookie.md`
2. Review the build logs: `./gradlew :desktop:build --stacktrace`
3. Verify `timekeeper` binary is working: `timekeeper status`

## Next Steps

1. **For Local Testing**:
   - Run `./build-macos-app.sh` on macOS
   - Install the app to Applications folder
   - Test all three menu items (Tick, Status, Exit)

2. **For Distribution**:
   - Sign the app with your Developer ID
   - Notarize the DMG file
   - Share the DMG link with users

3. **For Updates**:
   - Rebuild when you make code changes
   - Create new DMG files with updated version numbers
   - Consider implementing auto-update (future enhancement)
