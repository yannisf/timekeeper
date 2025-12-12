# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Timekeeper is a simple CLI time tracking application written in Kotlin. It tracks work sessions in a local SQLite database and provides commands to start/stop tracking, toggle tracking with a single command, and generate daily reports.

The database is stored at `~/.timekeeper.db` by default, but can be overridden with the `TIMEKEEPER_DB_PATH` environment variable.

## Architecture

The codebase is organized into a few key components:

- **TimeKeeper.kt**: Entry point and main function. Handles CLI argument parsing, validates input against the `Command` enum, and manages error codes.
- **Manager.kt**: Business logic layer. Coordinates between the CLI entry point and the database. Manages the state machine for start/stop operations (the `tick()` method toggles between these states). Contains utility functions for time formatting and duration pretty-printing.
- **Database.kt**: Data access layer. Handles all SQLite operations using JDBC. Contains the `TimeEntry` data class which maps database records to Kotlin objects.

### Key Design Patterns

- **State Management**: The application maintains a simple state: entries can be open (no stop time) or closed (with stop time). The `hasOpenEntryForDate()` check determines which operation is valid.
- **Error Handling**: Uses exit codes (1-4) defined in `ErrorCode` enum. Errors are printed to stderr and cause immediate process exit.
- **Time Formatting**: Uses `DateTimeFormatter` with pattern "HH:mm:ss" for time storage and formatting. Dates are stored as ISO strings (YYYY-MM-DD).

## Build and Development

### Build System

The project uses **Gradle 8.8** with Kotlin 2.2.21 and Java 21 toolchain.

### Common Commands

**Build:**
```bash
./gradlew build
```

**Run single command:**
```bash
./gradlew run --args='start' --rerun-tasks
./gradlew run --args='tick' --rerun-tasks
./gradlew run --args='report' --rerun-tasks
./gradlew run --args='stop' --rerun-tasks
```

**Run tests:**
```bash
./gradlew test
```

**Run single test:**
```bash
./gradlew test --tests TimeKeeperTest
```

**Create fat JAR:**
```bash
./gradlew shadowJar
java -jar app/build/libs/timekeeper.jar tick
```

**Build native executable (requires GraalVM):**
```bash
export GRAALVM_HOME=/path/to/graalvm
./gradlew -Pagent run --args="start"  # Agent mode for reflection config
./gradlew nativeCompile
./app/build/native/nativeCompile/timekeeper tick
```

### Dependencies

- **sqlite-jdbc 3.41.2.2**: SQLite database driver
- **junit-jupiter 5.10.1**: Testing framework
- **kotest-assertions 6.0.3**: Assertion library for tests

## Testing

The project has a single test class `TimeKeeperTest` that exercises the full workflow: initializing the database, starting, stopping, ticking, and generating reports. It uses console output capture with `captureConsoleOutput()` helper function to verify behavior.

When running tests locally, they use a temporary `./testdb` file that's cleaned up after execution.

## Database Schema

```sql
CREATE TABLE IF NOT EXISTS time_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    start TEXT NOT NULL,
    stop TEXT
)
```

Queries are defined as string constants in the `Database.Companion` object.

## GraalVM Native Compilation

The build includes special configuration for native compilation with GraalVM. Key settings:
- `--no-fallback`: Fail fast if unsupported features are encountered
- `-H:+JNI`: Enable JNI support for SQLite
- `--initialize-at-run-time`: Defers initialization of SQLite classes and JDBC to runtime to avoid issues with native reflection

To compile natively, you need GraalVM installed and the `GRAALVM_HOME` environment variable set.

## Key Files

- `app/src/main/kotlin/eu/frlab/timekeeper/TimeKeeper.kt`: CLI entry point
- `app/src/main/kotlin/eu/frlab/timekeeper/Manager.kt`: Business logic
- `app/src/main/kotlin/eu/frlab/timekeeper/Database.kt`: Data access
- `app/build.gradle.kts`: Build configuration with shadow JAR and GraalVM plugin setup
- `gradle/libs.versions.toml`: Dependency version catalog

## Environment Variables

- `TIMEKEEPER_DB_PATH`: Override the default database location (`~/.timekeeper.db`)
