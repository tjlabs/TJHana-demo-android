# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.12
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK (AAR): TJHana-sdk-android-1.0.1
<!-- HANA_SDK_AAR_VERSION_END -->

The app demonstrates Hana SDK flows with:
- Authentication (`AUTH`)
- Warp initialize/start/stop
- Warp view visibility and trigger interaction
- Venus initialize/start/stop
- Venus result callback handling
- Jupiter manager initialize/start/stop
- Jupiter destination/routing request

## Features

- Hana SDK auth/init/start/stop flow example
- Warp view attach by trigger (`FloatingActionButton`)
- Venus service result callback (`onVenusResult`)
- Jupiter service delegate callback (`onJupiterResult`, route events)
- Runtime permission request flow (Location/Bluetooth)
- Warp click callback and ward content URL parsing

## Requirements

- Android `minSdk 29+`
- Android Studio (latest stable recommended)
- Kotlin-based Android app

### Required permissions

Declare in `AndroidManifest.xml`:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.BLUETOOTH` (Android 11 and below)
- `android.permission.BLUETOOTH_ADMIN` (Android 11 and below)
- `android.permission.BLUETOOTH_SCAN` (Android 12+)

Runtime permission check in this demo requires:
- Location (`FINE`)
- Bluetooth scan on Android 12+

## Setup

### 1. Add repositories

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Place Hana AAR

Copy AAR file into:

```text
<!-- HANA_AAR_PATH_START -->
app/libs/TJHana-sdk-android-1.0.1.aar
<!-- HANA_AAR_PATH_END -->
```

If file name changes, update `hanaAarName` in `app/build.gradle.kts`.

### 3. Add dependencies

```kotlin
// app/build.gradle.kts
<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation(files("libs/TJHana-sdk-android-1.0.1.aar"))
    implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.12")
}
<!-- APP_DEPENDENCIES_END -->
```

## Quick Guide

### 1. Configure credentials

Set in `local.properties`:

```properties
sdk.dir=/Users/your_name/Library/Android/sdk
AUTH_ACCESS_KEY=YOUR_ACCESS_KEY
AUTH_SECRET_ACCESS_KEY=YOUR_SECRET_ACCESS_KEY
```

### 2. Authenticate

```kotlin
TJHanaAuth.auth(application, accessKey, accessSecretKey) { code, success ->
    // handle auth result
}
```

### 3. Initialize services

Auth success 이후:

```kotlin
warpView.initialize(id = userId, sectorId = sectorId)
venusManager.initialize(id = userId, sector_id = sectorId)
```

### 4. Start services

```kotlin
warpView.startService()
venusManager.startService()
```

### 5. Warp UI control

```kotlin
warpView.setVisibility(true)
warpView.setVisibility(false)
```

### 6. Stop services

```kotlin
warpView.stopService()
venusManager.stopService()
```

### 7. Jupiter manager (Hana SDK 1.0.1+)

```kotlin
val jupiterManager = TJJupiterManager(application, userId, sectorId, false)
jupiterManager.initialize()
jupiterManager.startService(UserMode.MODE_PEDESTRIAN)
```

Set destination:

```kotlin
jupiterManager.setNavigationDestination(Point(level_id = 1, x = 10, y = 10))
```

Request routing:

```kotlin
jupiterManager.requestRouting(
    start = RoutingStart(level_id = 1, x = 0, y = 0, absolute_heading = 0),
    destination = Point(level_id = 1, x = 10, y = 10),
    waypoints = emptyList(),
    requestType = RequestType.INIT,
    mergeLinks = false
)
```
