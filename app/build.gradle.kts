plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.graalvm)
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
