# Running Practice Companion on Your Android Device

This guide will walk you through the steps to run the Practice Companion app on your Android phone.

## Prerequisites

1. **Enable Developer Options on your phone**
   - Go to Settings > About phone
   - Tap on "Build number" 7 times until you see "You are now a developer" message
   - Go back to Settings > System > Developer options (or Settings > Developer options on some devices)
   - Enable "USB debugging"

2. **Install necessary drivers** (Windows only)
   - Download and install the OEM USB drivers for your specific Android device
   - Or install the Google USB Driver from the Android SDK Manager

## Connecting Your Device

1. **Connect your phone to your computer with a USB cable**

2. **Allow USB debugging**
   - When prompted on your phone, tap "Allow" to authorize USB debugging for your computer
   - Optionally check "Always allow from this computer" to skip this prompt in the future

3. **Verify the connection**
   - Run `adb devices` in a terminal or command prompt
   - You should see your device listed as "device"

## Building and Installing the App

### Method 1: Using Android Studio

1. **Open the project in Android Studio**

2. **Select your device from the device dropdown menu**
   - Make sure your phone appears in the dropdown list at the top of Android Studio
   - If not, check USB connection and debugging authorization

3. **Click the Run button (green triangle)**
   - Android Studio will build the app and install it on your phone
   - The app should automatically launch on your device

### Method 2: Using Gradle (Command Line)

1. **Navigate to the project directory**
   - Open a terminal or command prompt
   - Navigate to the root directory of the Practice Companion project

2. **Build and install the app**
   - Run `./gradlew installDebug` (on macOS/Linux)
   - Or `gradlew installDebug` (on Windows)
   - This will build the debug version and install it on your connected device

3. **Launch the app**
   - The app should appear in your app drawer
   - Tap on the Practice Companion icon to launch it

## Using the App Independently

Once installed, Practice Companion runs completely independently on your Android device:

1. **No computer connection required**
   - After installation, you can disconnect your phone from the computer
   - The app will continue to function normally with all features available

2. **Background operation**
   - The app can track your practice sessions even when your phone screen is off
   - A notification will show when a practice session is active

3. **Data persistence**
   - All your practice sessions are saved directly on your device
   - Your data remains available even if you restart your phone

4. **Reinstallation note**
   - If you uninstall the app, all saved practice data will be lost
   - Consider backing up important practice history before uninstalling

## Troubleshooting

### Device Not Detected

- Check that USB debugging is enabled
- Try a different USB cable or port
- Restart your phone and computer
- Try reinstalling the USB drivers (Windows)

### App Crashes on Launch

- Check the ADB logs with `adb logcat`
- Make sure your Android device meets the minimum requirements (Android 8.0+)
- Try clearing the app data in Settings > Apps > Practice Companion > Storage > Clear Data

### Permission Issues

- If the app requires additional permissions, you'll see prompts on your device
- Make sure to accept all necessary permissions, including microphone access for practice detection

## Development Workflow

1. **Make code changes** to the app in your development environment
2. **Rebuild and reinstall** using either Android Studio or the Gradle command
3. **Test the changes** on your physical device
4. **Check logs** using `adb logcat | grep "PracticeComp"` to filter for app-specific logs

## Advanced Features

### Wireless Debugging (Android 11+)

1. Connect your device via USB cable
2. In Developer Options, enable "Wireless debugging"
3. Tap on "Wireless debugging" > "Pair device with pairing code"
4. Run `adb pair <ip-address:port>` on your computer, using the IP and port shown on your device
5. Enter the pairing code shown on your device
6. Run `adb connect <ip-address:port>` (using the debugging port, not the pairing port)
7. Disconnect the USB cable and continue development wirelessly 