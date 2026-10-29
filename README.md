# TJHana-demo-android

## Overview

TJHana-demo-android is a minimal Android sample app for integrating **TJLabs Hana SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.43
<!-- JUPITER_SDK_VERSION_END -->

<!-- HANA_SDK_AAR_VERSION_START -->
Hana SDK: `com.github.tjlabs:TJHana-sdk-android:1.1.10` (JitPack)
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
    implementation("com.github.tjlabs:TJHana-sdk-android:1.1.10")
    // Jupiter SDK 는 Hana SDK 의 transitive dependency 로 자동 포함됩니다.
    // 명시적으로 pinning 하고 싶을 때만 아래 줄을 추가하세요.
    // implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.43")
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
    // handle selected wards — wards[0] 이 1등 (감지 세기 순)
}
```

`onWarpSelectionChanged` 가 오는 때 (1.1.10+):
- 선택된 ward 조합이 바뀔 때
- 조합은 같고 1등만 바뀔 때 — 새 1등이 마지막으로 전달된 1등보다 `leaderMarginDb` (기본 3 dB) 이상 높은
  상태가 `leaderDwellTicks` (기본 3회) 연속 유지되면

판정 기준은 기본값으로 충분합니다. 바꿔야 할 때만 (`setDecisionConfig` 를 호출하지 않으면 아래 기본값 그대로):

```kotlin
warpView.setDecisionConfig(
    TJWarpView.WarpDecisionConfig(
        qualificationWindowMillis = 5_000L, // 판정 창 — 이 안에서 임계값 이상 샘플로 판정 · 순위
        requiredHitCount = 4,               // 판정 창 안 최소 충족 샘플 수
        holdDurationMillis = 4_000L,        // 마지막 감지 후 유지 시간
        leaderMarginDb = 3.0,               // 같은 조합에서 1등 변경으로 볼 최소 차이 (dB)
        leaderDwellTicks = 3                // 위 조건이 연속 유지돼야 하는 횟수 (선택 주기 1회 = 1 tick)
    )
)
```

### 7. Stop services

```kotlin
warpView.stopService()
venusManager.stopService()
```

### 8. Jupiter manager (Hana SDK 1.1.9+)

> 좌표계: 입력 `Point` (목적지 · 경유지) 와 출력 (`JupiterResult.jupiter_pos` / `navi_pos`,
> `RoutingRoute`, `isNavigationRouteChanged`, `isWaypointChanged`) 모두 **하나 좌표계** 기준입니다.

```kotlin
val jupiterManager = TJJupiterManager(application, userId, sectorId, false)
jupiterManager.delegate = object : TJJupiterManagerDelegate { /* ... */ }
jupiterManager.initialize()
// resultIntervalMs(optional, default 1000ms): onJupiterResult 콜백 주기(ms)
jupiterManager.startService(resultIntervalMs = 1000, mode = UserMode.MODE_VEHICLE)

// 런타임 중 콜백 주기만 변경
jupiterManager.setResultInterval(milliseconds = 500)
```

Set destination:

```kotlin
// 하나 좌표계 · sector 1 B2 (level_id 6)
jupiterManager.setNavigationDestination(Point(level_id = 6, x = 7, y = -3))
```

Request routing (Notion 사양: end, waypoints, completion 3 파라미터):

```kotlin
jupiterManager.requestRouting(
    end = Point(level_id = 6, x = 7, y = -3),
    waypoints = emptyList()   // 목적지와 같은 층의 하나 좌표 Point
) { result ->
    // result.routes: List<RoutingRoute>  (하나 좌표계)
    // result.failureReason: NavigationRouteFailureReason?
}
```

- 출발지는 **현재 측위 위치** — `startService` 후 첫 `onJupiterResult` 이후에 호출합니다.
- completion 은 요청 1 건당 1 회 (응답 전에 다시 요청하면 이전 completion 은 호출되지 않음).
  성공이면 `routes`, 실패면 `routes` 가 비고 `failureReason` (1.1.10+ 세분화):

| `failureReason` | 의미 |
|---|---|
| `NO_CURRENT_POSITION` | 현재 위치(측위 결과)가 아직 없음 — `startService` 전 · 측위 전 |
| `LEVEL_NOT_FOUND` | 현재 위치의 층을 찾지 못함 |
| `SCALE_OFFSET_ERROR` | 목적지 · 경유지 층의 좌표 변환 정보 없음 |
| `TOO_CLOSE` | 같은 층에서 현재 위치와 목적지가 너무 가까움 (경유지 없이 직선거리가 최소 거리 이하) |
| `NETWORK_ERROR` | 서버에 닿지 못함 (타임아웃 · 연결 끊김) |
| `REQUEST_ERROR` | 요청을 만들지 못함 |
| `REQUEST_REJECTED` | 서버가 요청을 거절 (4xx — 예: 경로 없음) |
| `SERVER_ERROR` | 서버 오류 (5xx) |
| `INVALID_RESPONSE` | 서버 응답을 해석하지 못함 |
| `INVALID_ROUTE` | 받은 경로를 지도 정보로 구성하지 못함 |
| `NOT_READY` | 서비스가 준비되지 않음 |
| `CANCELLED` | 응답 전에 `stopService` |
| `UNKNOWN` | 그 밖의 값 |

- `INTERNAL_ERROR` · `SERVER_RESPONSE` 는 이전 버전 호환용으로 남아 있고 1.1.10 부터는 전달되지 않습니다.
- 요청 전 단계 실패 (`NO_CURRENT_POSITION` · `LEVEL_NOT_FOUND` · `SCALE_OFFSET_ERROR`) 는 SDK 가 진단용으로 서버에 기록합니다.
- 같은 경로와 이후 재탐색 경로는 `isNavigationRouteChanged` 로도 전달됩니다.
- on-prem 에서는 차량이 경로에서 **90 m** 넘게 벗어날 때만 이탈 (`isUserGuidanceOut`) · 재탐색합니다.

Delegate callbacks used in this demo:

```kotlin
override fun onInitSuccess(isSuccess: Boolean, errorCode: InitErrorCode?) { }
override fun onJupiterSuccess(isSuccess: Boolean, errorCode: JupiterErrorCode?) { }
override fun onJupiterResult(result: JupiterResult) { }
override fun isUserArrived() { }
override fun isNavigationRouteChanged(routes: List<JupiterNavigationRoute>) { }
override fun isNavigationRouteFailed() { }   // 이탈 재탐색 등 SDK 자동 요청 실패 포함 — 요청별 사유는 requestRouting completion
```

## Demo Defaults

- `demoUserId = "HanaUser01"`
- `demoSectorId = 1`
- `demoDestination = Point(level_id = 6, x = 7, y = -3)` (하나 좌표계 · B2)
- Jupiter `startService(resultIntervalMs = 1000, mode = UserMode.MODE_VEHICLE)`

## Migration Notes (1.1.9 → 1.1.10)

코드 변경 없이 버전만 올려도 빌드됩니다 (이 데모도 그대로). 확인할 점:

### 경로 요청 실패 사유 세분화

- `NavigationRouteFailureReason` 값 추가 — 위 [Jupiter manager](#8-jupiter-manager-hana-sdk-119) 표 참고.
  이전의 `INTERNAL_ERROR` (측위 결과 없음 · stopService) 는 `NO_CURRENT_POSITION` · `CANCELLED` 로,
  `SERVER_RESPONSE` (경로 서버 실패) 는 `REQUEST_REJECTED` · `SERVER_ERROR` · `INVALID_RESPONSE` · `INVALID_ROUTE` 등으로 나뉨.
- `failureReason` 을 `when` **식**(값을 반환하는 형태)으로 모든 값을 나열해 쓰고 있었다면 새 값 때문에
  컴파일 에러가 날 수 있으니 `else` 분기를 추가하세요. `INTERNAL_ERROR` · `SERVER_RESPONSE` 로만 분기하던 코드는
  새 값으로 바꿔야 실패 사유를 구분할 수 있습니다.

### Warp 선택 알림

- 조합이 같아도 1등이 바뀌면 `onWarpSelectionChanged` 가 옵니다 (위 [Warp UI control](#6-warp-ui-control) 참고) —
  이전보다 자주 올 수 있습니다.
- 판정 기본값 변경: 판정 창 10 s → 5 s, 최소 충족 샘플 6 → 4, 유지 시간 5 s → 4 s.
- `WarpDecisionConfig` 끝에 `leaderMarginDb` · `leaderDwellTicks` 추가 (기본값 있음 — 기존 호출 그대로 컴파일).

### 기타

- Warp · Venus 초기화 때 층 이미지를 받지 않습니다 (API 변경 없음).

### 하위 SDK 버전 (transitive, 1.1.10)

- Jupiter SDK `2.0.43` · Resource SDK `1.1.25` · Auth SDK `1.0.29` · Common SDK `1.0.32`

## Migration Notes (1.1.3 → 1.1.9)

### Warp 모델 필드명 정합화 (1.1.4, BREAKING)

| 이전 | 이후 |
|---|---|
| `WarpWard.ward_name` | `WarpWard.name` |
| `WarpWard.ward_rssi` | `WarpWard.rssi` |
| `WarpWard.ward_contents` | `WarpWard.contents` |
| `WardContents.contents_url` | `WardContents.url` |

- `WarpWard.detected_rssi: Int` 추가 (검출 세기값).
- `TJWarpView.restartScan()` 공개 (1.1.9).

### 좌표 — 모두 하나 좌표계

- Warp / Venus (1.1.4, BREAKING): 변환 방향 수정 — `hana = ours * scale + offset`
  (아래 1.1.0 노트의 `(px - offset) / scale` 은 더 이상 유효하지 않음). 앱에서 재변환하던 코드가 있으면 제거.
- **Jupiter (1.1.9, BREAKING)**: `jupiter_pos` · `navi_pos` (x · y · heading), 경로 · 경유지 결과,
  `requestRouting` / `setNavigationDestination` 입력 `Point` 모두 하나 좌표계.

### Jupiter (1.1.9)

- on-prem 실 Jupiter 연동 (mock 제거). `setMockMode` 는 Deprecated no-op.
- `requestRouting(end, waypoints, completion)` 실구현 — 출발지 = 현재 측위 위치, 서버 응답 후 completion.
- `JupiterResult.remain_distance` Deprecated · 항상 `null` (iOS parity).
- `requestRoutingWithOrigin` Deprecated.
- on-prem 차량 경로 이탈 기준 90 m.

### 하위 SDK 버전 (transitive, 1.1.9)

- Jupiter SDK `2.0.42` · Resource SDK `1.1.24` · Auth SDK `1.0.29` · Common SDK `1.0.32`

## Migration Notes (1.1.1)

Notion 사양 반영. 이전 버전 (1.1.0) 대비 변경점:

### Jupiter API 시그니처 변경

- `startService(resultIntervalMs = 1000, mode = ...)` — `resultIntervalMs` (기본 1000ms) 파라미터
  추가. `onJupiterResult` 콜백 주기를 밀리초 단위로 지정.
- `setResultInterval(milliseconds)` — 실행 중 콜백 주기를 런타임 변경.
- `requestRouting(end, waypoints, completion)` — 3 파라미터로 단순화. 기존
  `RoutingStart` / `RequestType` / `isVehicle` 파라미터는 제거.

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
