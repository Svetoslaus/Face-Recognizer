Overview
--------
This is an Android application for real-time face recognition using the power of ML Kit and CameraX. 
It processes camera frames, detects faces, and applies recognition logic on-device, making it fast and privacy-friendly.


Project Architecture
--------------------
The application follows a modular and maintainable architecture, inspired by MVVM (Model–View–ViewModel) and Clean Architecture patterns. 
Each package is responsible for a specific layer of functionality:

Model Layer (data/)
Manages data sources, including in-memory or persistent face data (FaceDatabase) and handles recognition logic through the RealtimeRecognitionRepo.

View Layer (ui/)
Contains the UI elements and camera interaction components. It’s separated into camera, components, and main for clarity and reuse.

ViewModel Layer (viewmodel/)
Acts as a bridge between the UI and the data. FaceRecognizer is likely used to handle ML processing and provide UI-friendly results.

Utility Classes (utils/)
Responsible for tasks like converting image formats and manipulating raw camera input to prepare for recognition.


Technical Specifications
-----------------------

Min SDK: 21

Target SDK: 34

Language: Java

ML Framework: Google ML Kit

Camera API: CameraX

IDE: Android Studio


Dependencies
------------
These dependencies must be defined in the build.gradle (Module: app):



    // AppCompat version for Java UI development
    implementation 'androidx.appcompat:appcompat:1.6.1'

    // UI libraries
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.material:material:1.9.0'

    // Testing dependencies (optional)
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'

    // ML dependencies
    implementation 'org.tensorflow:tensorflow-lite:+'
    implementation 'com.google.mlkit:face-detection:16.1.5'


Getting Started
---------------

Prerequisites
.............
Android Studio Hedgehog or newer
Android Emulator or Device with Camera
Java 8+


Build & Run
...........
Clone the repository
Open the project in Android Studio
Connect a device or launch an emulator
Click  ▶️ to run the app

Permission
----------

The app requires the following runtime permission:

<uses-permission android:name="android.permission.CAMERA"/>

Make sure to grant this when prompted.






