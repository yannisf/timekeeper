plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.graalvm)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.sqlite)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "eu.frlab.timekeeper.TimeKeeperKt"
}

tasks.shadowJar {
    archiveFileName.set("timekeeper.jar")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("timekeeper")
            mainClass.set(application.mainClass)

            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+JNI")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            buildArgs.add("--initialize-at-run-time=org.sqlite.core.NativeDB")
            buildArgs.add("--initialize-at-run-time=java.sql.DriverManager")
            buildArgs.add("--initialize-at-run-time=java.sql.Date")
            buildArgs.add("--initialize-at-run-time=java.sql.Timestamp")
        }
    }
}
