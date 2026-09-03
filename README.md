# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.14
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK: `com.github.tjlabs:TJHana-sdk-android:1.1.1` (JitPack)
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

### ⚠️ Network Security Config (필수)

Hana SDK 는 on-prem 서버 (HTTPS + 사설 CA) 로 접속합니다. Android 는 사설 CA 를 기본으로
신뢰하지 않으므로 **소비 앱에서 network security config 를 설정해야 합니다.**

#### 1) CA 인증서 파일 배치

서버 배포처로부터 받은 CA 인증서 (`.crt`) 를 아래 경로에 배치합니다:

```text
app/src/main/res/raw/tjlabs_hana_server_ca.crt
```

- 파일명은 소문자·숫자·언더스코어만 허용 (Android 리소스 명명 규칙)
- 파일은 저장소에 커밋하지 않는 것을 권장 (`.gitignore` 에 등록)

#### 2) `network_security_config.xml` 생성

`app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 하나 온프레미스 (HTTPS 사설 CA + HTTP 병행 허용) -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">HANA_SERVER_IP</domain>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
            <certificates src="@raw/tjlabs_hana_server_ca" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```

- `HANA_SERVER_IP` 는 배포처에서 공유받은 값을 사용
- `TJHanaEnvironment.setBaseUrl(...)` 로 다른 서버 IP 를 지정하는 경우 해당 IP 도 여기에 추가
- 다른 HTTP 전용 서버 (예: 사내 개발 서버) 는 별도 `<domain-config cleartextTrafficPermitted="true">`
  블록에 등록 (trust-anchors 생략)

#### 3) `AndroidManifest.xml` 참조 추가

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

#### SDK 가 자동 처리하는 부분

- **Hostname 검증 우회** (host-scope) — 인증서 CN 이 URL host 와 일치하지 않아도 on-prem host
  로 스코프 제한된 HostnameVerifier 가 자동 pass. cloud/외부 SaaS 통신엔 영향 없음.
- **URL rewrite** — 서버 응답의 raw bundle URL 에 `/api` prefix 자동 주입 (on-prem 모드).

#### 자주 발생하는 에러

- `CLEARTEXT communication to <ip> not permitted` — HTTP 호스트가 config 에 미등록
- `SSLHandshakeException: Trust anchor not found` — CA 인증서 (`@raw/tjlabs_hana_server_ca`) 미등록
- `SSLPeerUnverifiedException: Hostname ... not verified` — SDK 버전 확인 (1.1.0+ 에서 host-scope 우회 지원)

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

### 2. Add dependencies

```kotlin
// app/build.gradle.kts
<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation("com.github.tjlabs:TJHana-sdk-android:1.1.1")
    // Jupiter SDK 는 Hana SDK 의 transitive dependency 로 자동 포함됩니다.
    // 명시적으로 pinning 하고 싶을 때만 아래 줄을 추가하세요.
    // implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.29")
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

> ⚠️ **현재 Jupiter 는 mock-only 빌드입니다.** 온프렘 서버에 Jupiter 백엔드가 준비되기 전까지
> `initialize` / `startService` / `requestRouting` 은 실 서버 호출 없이 즉시 성공 콜백을 반환하고,
> `startService` 는 내부 SAMPLE 경로를 `resultIntervalMs` 주기로 `onJupiterResult` 로 스트리밍합니다.
> 정식 서버 연동 시 이 임시 처리는 제거됩니다.

```kotlin
val jupiterManager = TJJupiterManager(application, userId, sectorId, false)
jupiterManager.delegate = object : TJJupiterManagerDelegate { /* ... */ }
jupiterManager.initialize()
// resultIntervalMs(optional, default 1000ms): onJupiterResult 콜백 주기(ms)
jupiterManager.startService(resultIntervalMs = 1000, mode = UserMode.MODE_PEDESTRIAN)

// 런타임 중 콜백 주기만 변경
jupiterManager.setResultInterval(milliseconds = 500)
```

Set destination:

```kotlin
jupiterManager.setNavigationDestination(Point(level_id = 1, x = 10, y = 10))
```

Request routing (Notion 사양: end, waypoints, completion 3 파라미터):

```kotlin
jupiterManager.requestRouting(
    end = Point(level_id = 1, x = 10, y = 10),
    waypoints = emptyList()
) { result ->
    // result.routes: List<RoutingRoute>
    // result.failureReason: NavigationRouteFailureReason?
}
```

> ⚠️ Hana SDK 1.1.1 임시 동작: 온프렘 Jupiter 백엔드가 준비되기 전까지 SDK 는 mock-only 빌드로 동작합니다.
> - `initialize` / `startService` 는 즉시 성공 콜백을 반환하고, `startService` 는 내부 SAMPLE 경로(`level_id=700`, `"B2"`, 좌표 (70,10)→(70,18)→(10,18)→(10,29)→(5,29))를 `resultIntervalMs` 주기로 `onJupiterResult` 로 스트리밍합니다.
> - `requestRouting` 은 요청 내용과 무관하게 고정된 `SAMPLE_ROUTING_RESULT` 를 completion 으로 반환합니다.
> - `setMockMode` 는 `@Deprecated` no-op (현재 빌드는 항상 mock).

Delegate callbacks used in this demo:

```kotlin
override fun onInitSuccess(isSuccess: Boolean, errorCode: InitErrorCode?) { }
override fun onJupiterSuccess(isSuccess: Boolean, errorCode: JupiterErrorCode?) { }
override fun onJupiterResult(result: JupiterResult) { }
override fun isUserArrived() { }
override fun isNavigationRouteChanged(routes: List<JupiterNavigationRoute>) { }
```

## Demo Defaults

- `demoUserId = "HanaUser01"`
- `demoSectorId = 8`

## Migration Notes (1.1.1)

Notion 사양 반영 + 온프렘 대응 mock-only Jupiter 빌드. 이전 버전 (1.1.0) 대비 변경점:

### Jupiter — mock-only 빌드 유지

온프렘 서버에 Jupiter 백엔드가 준비되기 전까지, SDK 소비자가 호출 구조를 그대로 맞춰 개발할 수
있도록 `initialize` / `startService` / `requestRouting` 이 모두 즉시 성공 콜백을 반환한다.

- `startService(resultIntervalMs = 1000, mode = ...)` — 호출 즉시 mock `JupiterResult` 스트림 시작.
- `requestRouting(end, waypoints, completion)` — 요청 내용과 무관하게 고정 `SAMPLE_ROUTING_RESULT` 반환.
- `setResultInterval(milliseconds)` — 스트림 주기 런타임 변경.
- `setMockMode(flag)` — `@Deprecated` no-op (현재 빌드는 항상 mock).
- `RoutingStart`, `RequestType`, `isVehicle` 파라미터는 사양에서 삭제됨.

### Delegate 시그니처 변경 (TJJupiterManagerDelegate)

- `isNavigationRouteChanged(routeId, totalDistance, routes)` → `isNavigationRouteChanged(routes)` 로 단순화.
- `isUserArrived()` 콜백 신설 (목적지 도착).

### 모델 변경

- **`WarpWard.level_id: Int` 필드 신설** — 각 ward 가 속한 층 식별자가 함께 전달됨. `onClick` /
  `onWarpSelectionChanged` 로 오는 ward 는 서버 번들의 부모 level `id` 로 태깅.
- `PositionRequest` → **`Position`** 리네임 (`JupiterResult.jupiter_pos`, `navi_pos`).
- `JupiterNavigationRoute` 필드 순서 재배치 (buildingName, levelName, x, y 필수 + routeId /
  totalDistance / levelId / nodeNumber 는 nullable).
- `NavigationRouteFailureReason` 에 `INTERNAL_ERROR` / `SCALE_OFFSET_ERROR` 값 추가.

### 하위 SDK 버전 (transitive)

- Jupiter SDK: `2.0.29` (on-prem 지원 포함)
- Resource SDK: `1.1.11` (on-prem endpoint + URL rewrite + HostnameVerifier)
- Auth SDK: `1.0.28`
- Common SDK: `1.0.29`

## Migration Notes (1.1.0)

on-prem SDK 로 전환된 첫 정식 minor 릴리즈. 이전 버전 (1.0.x) 소비 코드에서 확인 필요한 항목:

### 좌표 필드 변경 — Warp / Venus 모두 **미터 실좌표**

- **`WarpWard.x`, `WarpWard.y`** — 필드는 유지되나 **타입이 `Int` (픽셀) → `Float` (미터)** 로 변경.
- **`VenusResult.x`, `VenusResult.y`** — 동일하게 `Int` → `Float` (미터).

두 필드는 번들의 `map_image.scale_x/y` · `offset_x/y` 가 적용된 실좌표입니다:

```
meter_x = (px_x - offset_x) / scale_x
meter_y = (px_y - offset_y) / scale_y    // scale_y 대개 음수 → y 뒤집힘
```

`Int` 로 받던 코드는 타입 미스매치로 컴파일 에러 발생 → `Float` 로 수정 필요.

### 서버 설정 API 변경

- **cloud 서버 설정 API 제거** (`TJHanaEnvironment.updateServerConfig`,
  `warp/jupiter/venusServerConfig` 등) — Hana SDK 는 on-prem 전용으로 전환.
- **기본 접속 서버 자동 활성화** — 소비 앱은 별도 설정 없이 `TJHanaAuth.auth(...)` 를 호출하면
  기본 하나 온프레미스 서버로 접속. 다른 서버 이관 시에만 `TJHanaEnvironment.setBaseUrl(url)`.
- 기존에 cloud API 를 호출하지 않았다면 코드 변경 불필요.

### 필수 세팅 (이전과 다름)

- **Network Security Config + CA 인증서** — 위
  [Network Security Config (필수)](#️-network-security-config-필수) 참고. HTTPS 사설 CA 를
  신뢰하려면 `res/raw/tjlabs_hana_server_ca.crt` 배치 + trust anchor 등록 필수.
