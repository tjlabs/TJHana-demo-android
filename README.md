# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.14
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK (AAR): TJHana-sdk-android-1.0.2
<!-- HANA_SDK_AAR_VERSION_END -->

The app demonstrates Hana SDK flows with:
- Authentication (`AUTH`)
- Warp initialize/start/stop
- Warp view visibility and trigger interaction
- Warp selection change callback
- Venus initialize/start/stop
- Venus result callback handling
- Jupiter manager initialize/start/stop
- Jupiter destination/routing request

## Features

- Hana SDK auth/init/start/stop flow example
- Warp view attach by trigger (`FloatingActionButton`)
- Warp ward selection callback (`onWarpSelectionChanged`)
- Venus service result callback (`onVenusResult`)
- Jupiter service delegate callback (`TJJupiterManagerDelegate`)
- Runtime permission request flow (Location/Bluetooth)
- Warp click callback and ward content URL parsing

## Requirements

- Android `minSdk 29+`
- Android Studio (latest stable recommended)
- Kotlin-based Android app

## Build Notes

- `release` build uses `isMinifyEnabled = true`
- Optional release signing is loaded from `local.properties` or Gradle properties:
  - `RELEASE_STORE_FILE`
  - `RELEASE_STORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`

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
app/libs/TJHana-sdk-android-1.0.2.aar
<!-- HANA_AAR_PATH_END -->
```

If file name changes, update `hanaAarName` in `app/build.gradle.kts`.

### 3. Add dependencies

```kotlin
// app/build.gradle.kts
<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation(files("libs/TJHana-sdk-android-1.0.2.aar"))
    implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.14")
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

This demo initializes Warp/Venus after auth. Jupiter is initialized separately by the `Init Jupiter Service` button.

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

Warp selection callback example:

```kotlin
override fun onWarpSelectionChanged(wards: List<WarpWard>) {
    // handle selected wards
}
```

### 6. Stop services

```kotlin
warpView.stopService()
venusManager.stopService()
```

### 7. Jupiter manager (Hana SDK 1.0.1+)

```kotlin
val jupiterManager = TJJupiterManager(application, userId, sectorId, false)
jupiterManager.delegate = object : TJJupiterManagerDelegate { /* ... */ }
jupiterManager.initialize()
jupiterManager.startService(UserMode.MODE_PEDESTRIAN)
```

Set destination:

```kotlin
jupiterManager.setNavigationDestination(Point(level_id = 1, x = 10, y = 10))
```

Request routing (SDK 1.0.6+ requires a completion callback):

```kotlin
jupiterManager.requestRouting(
    RoutingStart(level_id = 1, x = 0, y = 0, absolute_heading = 0),
    Point(level_id = 1, x = 10, y = 10),
    emptyList(),
    RequestType.INIT,
    false
) { result ->
    // result.routes: List<RoutingRoute>
    // result.failureReason: NavigationRouteFailureReason?
}
```

> ⚠️ Hana SDK 1.0.6 임시 동작: `requestRouting`은 요청 내용과 무관하게 항상 고정된 `SAMPLE_ROUTING_RESULT`를 completion으로 반환합니다. 정식 동작 전환 시 제거되는 임시 처리입니다.

### 8. Jupiter Mock Mode (Hana SDK 1.0.6+)

연동 테스트/UI 확인용으로 실제 측위 대신 **고정 경로를 따라가는 mock `JupiterResult`를 스트리밍**할 수 있습니다. 이 데모 앱에서는 화면의 **`Jupiter Mock Mode` 버튼**으로 토글할 수 있습니다.

```kotlin
// Jupiter가 initialize된 이후 호출
jupiterManager.setMockMode(true)   // 이후 startService에서 mock 스트림도 함께 시작
jupiterManager.startService(UserMode.MODE_PEDESTRIAN)
// ...
jupiterManager.stopService { _, _ -> } // mock 타이머까지 함께 정지
```

동작 요약:
- 간격 0.2초 (5 Hz), 가정 속도 4.0 m/s, 내부 SAMPLE 경로(`level_id=700`, `"B2"`, 좌표 (70,10)→(70,18)→(10,18)→(10,29)→(5,29))를 등속 리샘플링
- 각 tick의 `JupiterResult`는 `TJJupiterManagerDelegate.onJupiterResult(...)`로 메인 스레드 전달
- Mock 활성 동안 하위 SDK로부터 오는 실제 `onJupiterResult`는 무시됨
- 경로 끝 도달 시 자동 정지

> ⚠️ Mock은 개발/연동 확인용 임시 기능이며, 프로덕션 로직이 mock 결과에 의존하도록 만들지 마세요.

Delegate callbacks used in this demo:

```kotlin
override fun onInitSuccess(isSuccess: Boolean, errorCode: InitErrorCode?) { }
override fun onJupiterSuccess(isSuccess: Boolean, errorCode: JupiterErrorCode?) { }
override fun onJupiterResult(result: JupiterResult) { }
override fun isNavigationRouteChanged(
    routeId: String?,
    totalDistance: Int?,
    routes: MutableList<JupiterNavigationRoute>
) { }
```

## Demo Defaults

- `demoUserId = "HanaUser01"`
- `demoSectorId = 8`
