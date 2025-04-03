plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("jacoco")
}

android {
    namespace = "com.kailuowang.practicecomp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kailuowang.practicecomp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
                test.testLogging {
                    events = setOf(
                        org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT,
                        org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
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
    
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)

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
            // Use a simple approach to extract basic coverage info from the XML file
            val xmlContent = xmlFile.readText()
            
            // Create a map to store coverage data
            val coverageData = mutableMapOf<String, Any>()
            
            // Extract instruction coverage
            val instructionPattern = """<counter type="INSTRUCTION" missed="(\d+)" covered="(\d+)"/>""".toRegex()
            val instructionMatch = instructionPattern.find(xmlContent)
            
            if (instructionMatch != null) {
                val missed = instructionMatch.groupValues[1].toInt()
                val covered = instructionMatch.groupValues[2].toInt()
                val total = missed + covered
                val percentage = if (total > 0) (covered.toDouble() / total.toDouble() * 100.0) else 0.0
                
                coverageData["instructions"] = mapOf(
                    "missed" to missed,
                    "covered" to covered, 
                    "total" to total,
                    "percentage" to percentage
                )
                
                coverageData["overallCoverage"] = percentage
            }
            
            // Extract line coverage 
            val linePattern = """<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""".toRegex()
            val lineMatch = linePattern.find(xmlContent)
            
            if (lineMatch != null) {
                val missed = lineMatch.groupValues[1].toInt()
                val covered = lineMatch.groupValues[2].toInt()
                val total = missed + covered
                val percentage = if (total > 0) (covered.toDouble() / total.toDouble() * 100.0) else 0.0
                
                coverageData["lines"] = mapOf(
                    "missed" to missed,
                    "covered" to covered, 
                    "total" to total,
                    "percentage" to percentage
                )
            }
            
            // Extract branch coverage
            val branchPattern = """<counter type="BRANCH" missed="(\d+)" covered="(\d+)"/>""".toRegex()
            val branchMatch = branchPattern.find(xmlContent)
            
            if (branchMatch != null) {
                val missed = branchMatch.groupValues[1].toInt()
                val covered = branchMatch.groupValues[2].toInt()
                val total = missed + covered
                val percentage = if (total > 0) (covered.toDouble() / total.toDouble() * 100.0) else 0.0
                
                coverageData["branches"] = mapOf(
                    "missed" to missed,
                    "covered" to covered, 
                    "total" to total,
                    "percentage" to percentage
                )
            }
            
            // Add timestamp
            coverageData["timestamp"] = System.currentTimeMillis()
            
            // Write JSON to file
            val json = groovy.json.JsonOutput.toJson(coverageData)
            val prettyJson = groovy.json.JsonOutput.prettyPrint(json)
            
            jsonFile.parentFile.mkdirs()
            jsonFile.writeText(prettyJson)
            
            println("JSON coverage report generated at: ${jsonFile.absolutePath}")
        } else {
            println("XML report not found at: ${xmlFile.absolutePath}")
            throw GradleException("JaCoCo XML report not found. Run jacocoTestReport first.")
        }
    }
}