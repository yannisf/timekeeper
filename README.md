# Timekeeper

Timekeeper is a simple time tracking application designed to help users monitor their working time effectively.
It is not intended to be used to track time for billing or invoicing purposes,
but rather to provide insights into how time is spent during the workday.

## Usage

Timekeeper has two commands:

- `timekeeper tick` - Toggle between start and stop. Odd calls start a work session, even calls end it.
- `timekeeper status` - Display your current status and today's summary

**Caveat:** Timekeeper only tracks time within a day. If you forget to stop working one day, the entry
will remain open and the next time you resume, it will extend the same session if the break was short (default: 60 seconds).

## How it works

Internally, Timekeeper maintains a sqlite database in your home directory, under `~/.timekeeper.db`
with the following schema:

```sql
CREATE TABLE IF NOT EXISTS time_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    start TEXT NOT NULL,
    stop TEXT
)
```

Each time you run the `tick` command, a new entry is created or updated in this database.
The `status` command queries this database to show your current state.

The database location can be overridden by setting the `TIMEKEEPER_DB_PATH` environment variable. 
This might prove useful for testing or if you want to maintain multiple databases.

## How to run

You can build and run Timekeeper using gradle:

```bash
./gradlew build
./gradlew run --args='tick' --rerun-tasks 
```

Alternatively, you can create a fat jar and run it with java:

```bash
./gradlew shadowJar
java -jar app/build/libs/timekeeper.jar tick
```

Or, build a native executable using GraalVM:

```bash
export GRAALVM_HOME=/path/to/graalvm
./gradlew -Pagent run --args="tick"
./gradlew nativeCompile
./app/build/native/nativeCompile/timekeeper tick
```

## Smart Features

### Automatic Interval Extension
If you take a short break (default: 60 seconds), Timekeeper will automatically extend your current work session instead of creating a new one. This keeps your work log clean for brief interruptions.

Example:
```
11:00 - Start work
11:30 - Stop for a 45-second break
11:30:45 - Resume work → automatically extends the 11:00 session
```

### Short Interval Discard
Intervals shorter than the minimum duration (default: 60 seconds) are automatically discarded instead of being recorded. This prevents accidental entries from cluttering your data.

### Work Session Status
Use `timekeeper status` to see:
- Current state: whether you're working or on a break
- How long the current session/break has been going
- Total work time today
- Number of breaks taken

## How to analyze my data

A jupyter notebook will be provided in the `notebooks` directory to help you analyze your time tracking data.
