# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.29 (on-prem 지원 포함, JitPack)
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK: `com.github.tjlabs:TJHana-sdk-android:1.1.0` (JitPack)
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
    implementation("com.github.tjlabs:TJHana-sdk-android:1.1.0")
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

### Jupiter 상태

on-prem 모드에서 Jupiter positioning 은 아직 미지원 (REC/CALC 스펙 확정 대기). Warp / Venus 는
정상 동작.

### 필수 세팅 (이전과 다름)

- **Network Security Config + CA 인증서** — 위
  [Network Security Config (필수)](#️-network-security-config-필수) 참고. HTTPS 사설 CA 를
  신뢰하려면 `res/raw/tjlabs_hana_server_ca.crt` 배치 + trust anchor 등록 필수.

### 하위 SDK 버전 (transitive)

- Jupiter SDK: `2.0.29` (on-prem 지원 포함)
- Resource SDK: `1.1.11` (on-prem endpoint + URL rewrite + HostnameVerifier)
- Auth SDK: `1.0.28`
- Common SDK: `1.0.29`
