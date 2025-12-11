import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.coroutines.swing) // for test dispatchers
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.jediterm.pty)
                implementation(libs.jediterm.typeahead)
                implementation(libs.pty4j)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.colimator.app.MainKt"
        
        jvmArgs += listOf(
            "-Xdock:name=Colimator"  // Set app name in macOS menu bar for dev mode
        )

        buildTypes.release.proguard {
            obfuscate.set(false)
            optimize.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
        }


        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Colimator"
            packageVersion = project.findProperty("appVersion") as? String ?: "1.0.0"

            macOS {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.icns"))
            }
        }
    }
}

// Configure test logging to show stats in console
tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) { // root suite
                println("\n--------------------------------------------")
                println("Test Results: ${result.resultType}")
                println("Tests: ${result.testCount}, Passed: ${result.successfulTestCount}, Failed: ${result.failedTestCount}, Skipped: ${result.skippedTestCount}")
                println("--------------------------------------------")
            }
        }))
    }
}
