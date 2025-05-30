import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("jacoco")
}

// Function to increment and save the build number
fun getAndIncrementBuildNumber(): Int {
    val buildNumberFile = rootProject.file("buildNumber.properties")
    var buildNumber = 1
    
    if (buildNumberFile.exists()) {
        val properties = Properties()
        properties.load(buildNumberFile.inputStream())
        buildNumber = properties.getProperty("buildNumber", "1").toInt()
    }
    
    // Only increment build number for release builds or when explicitly requested
    if (gradle.startParameter.taskNames.any { it.contains("Release") } ||
        project.hasProperty("incrementBuildNumber")) {
        // Increment for next build
        buildNumber++
        
        // Save the incremented number
        val properties = Properties()
        properties.setProperty("buildNumber", buildNumber.toString())
        buildNumberFile.outputStream().use { properties.store(it, null) }
    }
    
    return buildNumber
}

android {
    namespace = "com.kailuowang.practicecomp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kailuowang.practicecomp"
        minSdk = 26
        targetSdk = 34
        versionCode = getAndIncrementBuildNumber()
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // Add this line
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                test ->
                // Enable Robolectric logging to stdout
                test.systemProperty("robolectric.logging.enabled", "true")
                // Force Robolectric logging to stdout
                test.systemProperty("robolectric.logging", "stdout")

                test.testLogging {
                    events = setOf(
                        org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT, // Ensure stdout is logged
                        org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR // Ensure stderr is logged
                    )
                    showStandardStreams = true
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                }
            }
            
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.generativeai)
    implementation(libs.androidx.navigation.compose)
    
    implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // Explicitly add AutoValue annotations dependency (often needed with ProGuard)
    implementation("com.google.auto.value:auto-value-annotations:1.10.1")
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)

    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.kotlin)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

jacoco {
    toolVersion = "0.8.12"
}

val kotlinClasses = fileTree("$buildDir/tmp/kotlin-classes/debug") {
    exclude(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/Dagger*Component*.*",
        "**/*Module*.*",
        "**/*_Factory*.*",
        "**/*_MembersInjector*.*",
        "**/*Args*.*",
        "**/*Directions*.*",
        "**/*Composable*.*",
        "**/*Activity*.*",
        "**/*Fragment*.*",
        "**/*Adapter*.*",
        "**/*ViewHolder*.*",
        "**/*Application*.*",
        "**/ui/theme/**",
        "**/PracticeAppContainer*.*",
        "**/Clock*.*"
    )
}

val sourceDirs = files("$projectDir/src/main/java", "$projectDir/src/main/kotlin")

tasks.register("jacocoTestReport", JacocoReport::class) {
    group = "verification"
    description = "Generates JaCoCo code coverage reports for the debug unit tests."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
        csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.csv"))
    }

    executionData.setFrom(fileTree(buildDir) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })

    classDirectories.setFrom(files(kotlinClasses))

    sourceDirectories.setFrom(sourceDirs)
}

// Replace the previous generateCoverageJson task with this Kotlin-compatible version

tasks.register("generateCoverageJson") {
    description = "Generates a simplified JSON coverage report"
    group = "verification"
    
    dependsOn("jacocoTestReport")
    
    doLast {
        val xmlFile = file("$buildDir/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        val jsonFile = file("$buildDir/reports/jacoco/coverage-summary.json")
        
        if (xmlFile.exists()) {
            // Use a better approach to extract the full coverage metrics
            val xmlContent = xmlFile.readText()
            
            // Create a map to store coverage data
            val coverageData = mutableMapOf<String, Any>()
            
            // Extract coverage for all counter types
            val counterTypes = listOf("INSTRUCTION", "BRANCH", "LINE", "COMPLEXITY", "METHOD", "CLASS")
            val countersMap = mutableMapOf<String, Map<String, Any>>()
            
            // Process each counter type
            for (type in counterTypes) {
                // Look for the report-level counter (should be at the end of the file, so using lastMatch)
                val pattern = """<counter type="$type" missed="(\d+)" covered="(\d+)"/>""".toRegex()
                val allMatches = pattern.findAll(xmlContent).toList()
                
                // If there are matches, use the last one (typically the summary counter)
                if (allMatches.isNotEmpty()) {
                    val match = allMatches.last()
                    val missed = match.groupValues[1].toInt()
                    val covered = match.groupValues[2].toInt()
                    val total = missed + covered
                    val percentage = if (total > 0) (covered.toDouble() / total.toDouble() * 100.0) else 0.0
                    
                    // Store in a normalized format (lowercase key)
                    val key = type.lowercase()
                    countersMap[key] = mapOf(
                        "missed" to missed,
                        "covered" to covered, 
                        "total" to total,
                        "percentage" to percentage
                    )
                }
            }
            
            // Add all counters to the coverage data
            coverageData["coverage"] = countersMap
            
            // Set overall coverage based on instruction coverage (industry standard)
            val instructionCoverage = countersMap["instruction"]
            if (instructionCoverage != null) {
                coverageData["overallCoverage"] = instructionCoverage["percentage"] ?: 0.0
            } else {
                coverageData["overallCoverage"] = 0.0
            }
            
            // Add project information
            coverageData["project"] = mapOf(
                "name" to "PracticeComp",
                "timestamp" to System.currentTimeMillis()
            )
            
            // Write JSON to file
            val json = groovy.json.JsonOutput.toJson(coverageData)
            val prettyJson = groovy.json.JsonOutput.prettyPrint(json)
            
            jsonFile.parentFile.mkdirs()
            jsonFile.writeText(prettyJson)
            
            // Print summary to console
            println("JSON coverage report generated at: ${jsonFile.absolutePath}")
            println("Overall coverage: ${coverageData["overallCoverage"]}%")
        } else {
            println("XML report not found at: ${xmlFile.absolutePath}")
            throw GradleException("JaCoCo XML report not found. Run jacocoTestReport first.")
        }
    }
}