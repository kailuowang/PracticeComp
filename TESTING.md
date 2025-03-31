# Testing the Application

This document outlines how to run the automated tests included in this Android project. There are two main types of tests: Local Unit Tests and Instrumented Tests.

## 1. Local Unit Tests

These tests run directly on your development machine's Java Virtual Machine (JVM) without needing an Android device or emulator. They are typically fast and used for testing logic that doesn't depend on the Android framework.

- **Location:** `app/src/test/java/`
- **Example:** `ExampleUnitTest.kt`

### Running Local Unit Tests:

#### From Android Studio / IntelliJ / Cursor:
1.  Open the test file (e.g., `app/src/test/java/com/kailuowang/practicecomp/ExampleUnitTest.kt`).
2.  Click the green 'play' icon in the gutter next to the class name or a specific test method (`@Test`).
3.  Select 'Run <TestName>'.

#### From the Command Line (Gradle):
1.  Open a terminal in the project's root directory (`/Users/kailuowang/projects/PracticeComp`).
2.  Run the following command:
    ```bash
    ./gradlew testDebugUnitTest
    ```
    (Replace `Debug` with the desired build variant if necessary, e.g., `testReleaseUnitTest`).

## 2. Instrumented Tests

These tests run on a physical Android device or an emulator. They are necessary for testing code that interacts with the Android framework APIs, including UI components, Context, etc.

- **Location:** `app/src/androidTest/java/`
- **Example:** `ExampleInstrumentedTest.kt`

### Running Instrumented Tests:

#### From Android Studio / IntelliJ / Cursor:
1.  Ensure a physical device is connected or an emulator is running.
2.  Open the test file (e.g., `app/src/androidTest/java/com/kailuowang/practicecomp/ExampleInstrumentedTest.kt`).
3.  Click the green 'play' icon in the gutter next to the class name or a specific test method (`@Test`).
4.  Select 'Run <TestName>'. You might be prompted to choose a target device.

#### From the Command Line (Gradle):
1.  Ensure a physical device is connected or an emulator is running and visible via `adb devices`.
2.  Open a terminal in the project's root directory.
3.  Run the following command:
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```
    (Replace `Debug` with the desired build variant if necessary, e.g., `connectedReleaseAndroidTest`).

---

Test results (especially for command-line runs) can often be found in detail within the `app/build/reports/` directory. 