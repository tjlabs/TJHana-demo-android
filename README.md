# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.28-onprem-SNAPSHOT (on-prem 지원 브렌치, mavenLocal)
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK (AAR): TJHana-sdk-android-1.0.7
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

### ⚠️ Cleartext HTTP (필수)

Hana SDK 는 on-prem PMS 서버 (`http://<host>:<port>`) 로 접속합니다. Android 9(API 28)+ 는
평문 통신을 기본 차단하므로 **소비 앱에서 network security config 를 설정해야 합니다.**

`app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <!-- 하나 온프레미스 서버 IP (배포처에서 공유받은 값 사용) -->
        <domain includeSubdomains="false">HANA_SERVER_IP</domain>
        <!-- 다른 서버 (예: 사내 개발) 사용 시 해당 IP 추가 -->
    </domain-config>
</network-security-config>
```

`AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

`TJHanaEnvironment.setBaseUrl(...)` 로 다른 서버 IP 를 지정하는 경우, 해당 IP 도 위 config 에
추가해야 합니다. 등록되지 않은 cleartext 호스트로 요청 시 다음 에러가 발생합니다:

```
CLEARTEXT communication to <ip> not permitted by network security policy
```

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
app/libs/TJHana-sdk-android-1.0.7.aar
<!-- HANA_AAR_PATH_END -->
```

If file name changes, update `hanaAarName` in `app/build.gradle.kts`.

### 3. Add dependencies

```kotlin
// app/build.gradle.kts
<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation(files("libs/TJHana-sdk-android-1.0.7.aar"))
    // on-prem 지원 브렌치는 mavenLocal 스냅샷. 정식 릴리즈 후 -onprem-SNAPSHOT suffix 제거.
    implementation("com.tjlabs:TJLabsJupiter-sdk-android:2.0.28-onprem-SNAPSHOT")
}
<!-- APP_DEPENDENCIES_END -->
```

`settings.gradle.kts` 의 `dependencyResolutionManagement.repositories` 에 `mavenLocal()` 추가 필요
(정식 릴리즈 전까지 스냅샷 배포용).

## Quick Guide

### 1. Configure credentials

Set in `local.properties`:

```properties
sdk.dir=/Users/your_name/Library/Android/sdk
AUTH_ACCESS_KEY=YOUR_ACCESS_KEY
AUTH_SECRET_ACCESS_KEY=YOUR_SECRET_ACCESS_KEY
```

### 2. (선택) 서버 URL 오버라이드

Hana SDK 는 기본으로 하나 온프레미스 서버로 접속합니다 (base URL 은 배포처에서 별도 공유).
다른 서버 (예: 사내 개발/테스트) 를 쓰려면 auth 이전에:

```kotlin
TJHanaEnvironment.setBaseUrl("http://<other-server>:<port>")
```

새 IP 를 `network_security_config.xml` 에도 등록해야 합니다. (위 [필수] 절 참고)

### 3. Authenticate

```kotlin
TJHanaAuth.auth(application, accessKey, accessSecretKey) { code, success ->
    // handle auth result
}
```

### 4. Initialize services

Auth success 이후:

```kotlin
warpView.initialize(id = userId, sectorId = sectorId)
venusManager.initialize(id = userId, sector_id = sectorId)
```

This demo initializes Warp/Venus after auth. Jupiter is initialized separately by the `Init Jupiter Service` button.

### 5. Start services

```kotlin
warpView.startService()
venusManager.startService()
```

### 6. Warp UI control

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

### 7. Stop services

```kotlin
warpView.stopService()
venusManager.stopService()
```

### 8. Jupiter manager (Hana SDK 1.0.1+)

> ⚠️ **on-prem 모드에서는 Jupiter positioning 이 현재 지원되지 않습니다.** Jupiter 는 REC/CALC
> endpoint 를 사용하는데 on-prem PMS 스펙이 아직 확정되지 않았습니다. Warp / Venus 는 정상
> 동작하며, Jupiter API 는 다음 릴리즈에서 on-prem 지원 예정.

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

### 9. Jupiter Mock Mode (Hana SDK 1.0.6+)

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

## Migration Notes (1.0.7)

이전 버전 (1.0.6 이하) 소비 코드가 아래 필드에 접근하고 있으면 조정 필요:

- **`WarpWard.x`, `WarpWard.y` 제거**  
  Warp 는 근접(proximity) 서비스로 좌표 개념이 없어 해당 필드를 삭제. 컴파일 시 해당 참조를
  제거해야 합니다. 좌표가 필요하면 Venus 를 사용하세요.

- **`VenusResult.x`, `VenusResult.y` 타입 변경 (`Int` → `Float`)**  
  이전엔 픽셀 좌표(정수) 였으나, 이제 번들의 `map_image.scale_x/y` · `offset_x/y` 가 적용된
  **미터 단위 실좌표(Float)** 를 제공합니다. 픽셀→미터 변환:  
  `meter_x = (px_x - offset_x) / scale_x`, `meter_y = (px_y - offset_y) / scale_y`  
  `Int` 로 받던 코드는 타입 미스매치로 컴파일 에러 발생 → `Float` 로 수정.

- **cloud 서버 설정 API 제거** (`TJHanaEnvironment.updateServerConfig` 등)  
  Hana SDK 는 이제 on-prem 전용입니다. 기존에 이 API 를 호출하지 않았다면 영향 없음.
  다른 서버 URL 로 이관하려면 [Quick Guide #2](#2-선택-서버-url-오버라이드) 참고.

- **`network_security_config.xml` 필수** — 위 [Cleartext HTTP (필수)](#️-cleartext-http-필수) 참고.
