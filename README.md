# Battery Guardian App

## Overview
The Battery Guardian app is designed to provide a critical alert when your Android device's battery level drops to 1%. It displays a prominent, impossible-to-miss countdown timer over all other applications, urging you to plug in your device before it dies.

## Core Features
-   **1% Battery Detection:** Monitors battery level specifically for the 1% threshold.
-   **Instant Overlay:** Immediately shows a customizable countdown timer over all apps.
-   **Persistent Timer:** Stays visible until the battery dies or the device is plugged in.
-   **Prominent Display:** Features a large, clear countdown with an urgent message.
-   **Enable/Disable Toggle:** Easily turn the app's monitoring on or off.
-   **Adjustable Timer Duration:** Set the countdown from 1 to 5 minutes.
-   **Customizable Timer Style:** Choose between different visual styles for the overlay.
-   **Sound Alert:** Optional beep when the timer starts.
-   **Charger Connected Detection:** The timer automatically disappears if you plug in during the countdown.
-   **Restart Protection:** Automatically restarts battery monitoring after device reboot.

## Technical Requirements
The application requires the following permissions:
-   `android.permission.SYSTEM_ALERT_WINDOW`: To draw the overlay over other apps.
-   `android.permission.FOREGROUND_SERVICE`: To run the battery monitoring service in the background.
-   `android.permission.RECEIVE_BOOT_COMPLETED`: To restart the service after device reboot.
-   `android.permission.WAKE_LOCK`: To prevent the device from sleeping during critical operations.
-   `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: To ensure the app runs uninterrupted by battery optimizations.

## How to Build and Run

### Using Android Studio (Recommended)
1.  **Open Project:** Launch Android Studio and select "Open an existing Android Studio project". Navigate to the project's root directory (`D:\ExtractionComplete\BatteryGaurdian\Gaurdian`) and click "OK".
2.  **Gradle Sync:** Android Studio will automatically sync the Gradle project. Ensure your JDK is set to version 24 (or compatible). The `gradle.properties` file should contain `org.gradle.java.home=C:/Program Files/Java/jdk-24` (adjust path if your JDK is installed elsewhere).
3.  **Run:** Select your desired Android emulator or a connected physical device from the target device dropdown in the toolbar. Click the green "Run" button (play icon) to build and deploy the application.

### Using Command Line
1.  **Navigate to Project Root:** Open your terminal or command prompt and navigate to the project's root directory:
    ```bash
    cd D:\ExtractionComplete\BatteryGaurdian\Gaurdian
    ```
2.  **Build Debug APK:** To build the debug version of the application, run:
    ```bash
    ./gradlew assembleDebug
    ```
    (On Windows, use `gradlew.bat assembleDebug`)
3.  **Install on Device/Emulator:** To install the built APK on a connected device or running emulator, run:
    ```bash
    ./gradlew installDebug
    ```
    (On Windows, use `gradlew.bat installDebug`)

## Testing
After installing the app, launch it on your device/emulator.
-   **Settings:** Verify that you can adjust the timer duration, toggle sound alerts, and change visual styles. Ensure these settings persist.
-   **Core Functionality:**
    -   Enable the Battery Guardian service.
    -   Simulate a 1% battery level (e.g., using developer options or a battery simulation tool).
    -   Observe the countdown overlay appearing.
    -   Plug in the charger during the countdown to confirm the overlay disappears.
    -   Reboot your device and check if the service automatically restarts.

## Future Enhancements
-   More diverse visual styles for the countdown overlay.
-   Customizable sound alert options.
-   Improved UI/UX for settings and overall app interaction.
-   Additional battery level triggers (e.g., 5%, 10%).
-   User-defined custom messages for the overlay.
