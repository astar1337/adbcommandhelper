import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"      // was 1.9.0
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.json:json:20231013")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0-beta04")
    implementation("androidx.compose.remote:remote-creation-core:1.0.0-alpha18")
}

val generateConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/config")
    val prodKey = localProperties.getProperty("PROD_API_KEY", "")
    val stagKey = localProperties.getProperty("STAG_API_KEY", "")
    inputs.property("prodKey", prodKey)
    inputs.property("stagKey", stagKey)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("AppConfig.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package config

                object AppConfig {
                    const val PROD_API_KEY: String = "$prodKey"
                    const val STAG_API_KEY: String = "$stagKey"
                }
                """.trimIndent()
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(generateConfig)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Android Helper"
            packageVersion = "2.0.4"
            macOS {
                bundleID = "com.dazn.androidhelper"
                iconFile.set(project.file("src/main/resources/icons/andh.icns"))
            }
        }
    }
}

tasks.register<Exec>("signApp") {
    dependsOn("createDistributable")
    commandLine(
        "codesign", "--force", "--deep", "--sign", "-",
        layout.buildDirectory
            .dir("compose/binaries/main/app/Android Helper.app")
            .get().asFile.absolutePath
    )
}