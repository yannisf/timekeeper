import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.compose)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)

    // Coroutines for async operations
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

compose.desktop {
    application {
        mainClass = "eu.frlab.timekeeper.desktop.DesktopAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = "Timekeeper"
            packageVersion = "1.0.0"
            description = "Time tracking system tray application"
            vendor = "frlab.eu"

            macOS {
                bundleID = "eu.frlab.timekeeper.desktop"
                iconFile.set(project.file("src/main/resources/icons/app-icon.icns"))

                // Run in background mode (no dock icon)
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <true/>
                        <key>LSMinimumSystemVersion</key>
                        <string>10.13</string>
                    """
                }
            }
        }
    }
}
