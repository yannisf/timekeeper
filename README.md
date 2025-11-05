# Timekeeper

Timekeeper is a simple time tracking application designed to help users monitor their working time effectively.
It is not intended to be used to track time for billing or invoicing purposes,
but rather to provide insights into how time is spent during the workday.

## Usage

Everytime you start or stop working, simply run `timekeeper tick` in your terminal. 
The odd times you run the command will be recorded as start times and the even times will be recorded as stop times.
If you'd rather want to be explicit that you start or stop working, you can use `timekeeper start` and `timekeeper stop` respectively.

**Caveat:** Timekeeper only tracks time within a day. If you forget to stop working one day, the entry
will remain open and the next time you start working, a new entry will be created.

## How it works

Internally, Timekeeper maintains a sqlite database in your home directory, under `~/.timekeeper/timekeeper.db` 
with the following schema:

```sql
CREATE TABLE IF NOT EXISTS time_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    start TEXT NOT NULL,
    stop TEXT
)
```

Each time you run the `tick`, `start`, or `stop` commands, a new entry is created or updated in this database.

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
./gradlew -Pagent run --args="start"
./gradlew nativeCompile
./app/build/native/nativeCompile/timekeeper tick
```

## How to analyze my data

A jupyter notebook will be provided in the `notebooks` directory to help you analyze your time tracking data.
