package com.tjlabs.tjhana_demo_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tjlabs.tjhana_sdk_android.InOutState
import com.tjlabs.tjhana_sdk_android.InitErrorCode
import com.tjlabs.tjhana_sdk_android.JupiterErrorCode
import com.tjlabs.tjhana_sdk_android.JupiterNavigationRoute
import com.tjlabs.tjhana_sdk_android.JupiterResult
import com.tjlabs.tjhana_sdk_android.JupiterServiceCode
import com.tjlabs.tjhana_sdk_android.Point
import com.tjlabs.tjhana_sdk_android.RequestType
import com.tjlabs.tjhana_sdk_android.RoutingStart
import com.tjlabs.tjhana_sdk_android.TJHanaAuth
import com.tjlabs.tjhana_sdk_android.TJHanaLogger
import com.tjlabs.tjhana_sdk_android.TJJupiterManager
import com.tjlabs.tjhana_sdk_android.TJJupiterManagerDelegate
import com.tjlabs.tjhana_sdk_android.TJVenusManager
import com.tjlabs.tjhana_sdk_android.TJVenusManagerDelegate
import com.tjlabs.tjhana_sdk_android.TJWarpView
import com.tjlabs.tjhana_sdk_android.TJWarpViewDelegate
import com.tjlabs.tjhana_sdk_android.UserMode
import com.tjlabs.tjhana_sdk_android.VenusErrorCode
import com.tjlabs.tjhana_sdk_android.VenusInitErrorCode
import com.tjlabs.tjhana_sdk_android.VenusResult
import com.tjlabs.tjhana_sdk_android.WarpErrorCode
import com.tjlabs.tjhana_sdk_android.WarpInitErrorCode
import com.tjlabs.tjhana_sdk_android.WarpWard

class MainActivity : AppCompatActivity() {
    private val logTag = "HanaDemo/Jupiter"
    private lateinit var resultTextView: TextView
    private lateinit var warpView: TJWarpView
    private lateinit var venusManager: TJVenusManager
    private lateinit var jupiterManager: TJJupiterManager

    private val permissionRequestCode = 1001
    private val demoUserId = "HanaUser01"
    private val demoSectorId = 8
    private var isAuthCompleted = false
    private var isWarpInitialized = false
    private var isVenusInitialized = false
    private var isJupiterInitialized = false
    private var isJupiterMockEnabled = false
    private var authStatusText = "Auth: 대기"
    private var warpInitStatusText = "Warp init: 대기"
    private var venusInitStatusText = "Venus init: 대기"
    private var jupiterInitStatusText = "Jupiter init: 대기"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        applySystemInsets()
        requestRuntimePermissionsIfNeeded()

        resultTextView = findViewById(R.id.tv_result)

        TJHanaLogger.setInternalTestMode(true)
        initializeManagers()
        bindButtons()
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeManagers() {
        initializeWarpManager()
        initializeVenusManager()
        initializeJupiterManager()
    }

    
    private fun initializeWarpManager() {
        val triggerFab = findViewById<FloatingActionButton>(R.id.fab_trigger)

        warpView = TJWarpView(this).apply {
            delegate = object : TJWarpViewDelegate {
                override fun onInitSuccess(isSuccess: Boolean, code: WarpInitErrorCode?) {
                    isWarpInitialized = isSuccess
                    warpInitStatusText = "Warp init: success=$isSuccess, code=$code"
                    showInitStatus()
                }

                override fun onWarpSelectionChanged(wards: List<WarpWard>) {
                    showResult(buildWarpSelectionText(wards))
                }

                override fun onWarpSuccess(isSuccess: Boolean, code: WarpErrorCode) {
                    showResult("[Warp] 서비스 결과: success=$isSuccess, code=$code")
                }

                override fun onClick(wards: List<WarpWard>) {
                    showResult(buildWarpClickText(wards))
                }
            }
        }
        warpView.setSelectionInterval()
        triggerFab.post { warpView.configureFrame(triggerFab) }
    }

    private fun initializeVenusManager() {
        venusManager = TJVenusManager(this).apply {
            delegate = object : TJVenusManagerDelegate {
                override fun onInitSuccess(isSuccess: Boolean, code: VenusInitErrorCode?) {
                    isVenusInitialized = isSuccess
                    venusInitStatusText = "Venus init: success=$isSuccess, code=$code"
                    showInitStatus()
                }

                override fun onVenusSuccess(isSuccess: Boolean, code: VenusErrorCode?) {
                    showResult("[Venus] service: success=$isSuccess, code=$code")
                }

                override fun onVenusResult(result: VenusResult) {
                    showResult(
                        """
                        [Venus] result
                        mobile_time=${result.mobile_time}
                        building_id=${result.building_id}
                        building_name=${result.building_name}
                        level_id=${result.level_id}
                        level_name=${result.level_name}
                        x=${result.x}, y=${result.y}
                        """.trimIndent()
                    )
                }
            }
        }
    }

    private fun initializeJupiterManager() {
        jupiterManager = TJJupiterManager(application, demoUserId, demoSectorId, false).apply {
            delegate = object : TJJupiterManagerDelegate {
                override fun onInitSuccess(isSuccess: Boolean, errorCode: InitErrorCode?) {
                    TJHanaLogger.i(logTag, "[JUPITER_INIT] result success=$isSuccess, code=$errorCode")
                    isJupiterInitialized = isSuccess
                    jupiterInitStatusText = "Jupiter init: success=$isSuccess, code=$errorCode"
                    showInitStatus()
                }

                override fun onJupiterSuccess(isSuccess: Boolean, errorCode: JupiterErrorCode?) {
                    showResult("[Jupiter] service: success=$isSuccess, code=$errorCode")
                }

                override fun onJupiterReport(code: JupiterServiceCode, message: String) {
                    showResult("[Jupiter] report: code=$code, message=$message")
                }

                override fun onJupiterResult(result: JupiterResult) {
                    showResult("[Jupiter] result: $result")
                }

                override fun isJupiterInOutStateChanged(inOutState: InOutState) {
                    showResult("[Jupiter] inOutState: $inOutState")
                }

                override fun isUserGuidanceOut() {
                    showResult("[Jupiter] user guidance out")
                }

                override fun isNavigationRouteChanged(
                    routeId: String?,
                    totalDistance: Int?,
                    routes: List<JupiterNavigationRoute>
                ) {
                    showResult("[Jupiter] route changed: count=${routes.size}")
                }

                override fun isNavigationRouteFailed() {
                    showResult("[Jupiter] route failed")
                }

                override fun isWaypointChanged(waypoints: List<List<Double>>) {
                    showResult("[Jupiter] waypoint changed: count=${waypoints.size}")
                }
            }
        }
    }

    private fun authenticateAndInitializeSdk() {
        val accessKey = BuildConfig.AUTH_ACCESS_KEY.trim()
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY.trim()
        TJHanaLogger.i(logTag, "[AUTH] request started")

        TJHanaAuth.auth(application, accessKey, accessSecretKey) { code, success ->
            if (!success) {
                TJHanaLogger.w(logTag, "[AUTH] failed code=$code")
                authStatusText = "Auth: 실패(code=$code)"
                showInitStatus()
                Toast.makeText(this, "Auth failed: $code", Toast.LENGTH_SHORT).show()
            } else {
                TJHanaLogger.i(logTag, "[AUTH] success code=$code")
                isAuthCompleted = true
                authStatusText = "Auth: 성공(code=$code)"
                initializeAllServices()
                showInitStatus()
            }
        }
    }

    private fun initializeAllServices() {
        warpView.initialize(id = demoUserId, sectorId = demoSectorId)
        venusManager.initialize(id = demoUserId, sector_id = demoSectorId)
    }

    private fun bindButtons() {
        findViewById<Button>(R.id.btn_auth).setOnClickListener {
            if (isAuthCompleted) {
                showResult("이미 Auth 완료됨")
                return@setOnClickListener
            }
            authenticateAndInitializeSdk()
        }

        findViewById<Button>(R.id.btn_visibility_test).setOnClickListener {
            if (!isWarpInitialized) return@setOnClickListener
            warpView.setVisibility(!warpView.getVisibilityState())
            showResult("[Warp] Visibility: ${warpView.getVisibilityState()}")
        }

        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            if (!isWarpInitialized) return@setOnClickListener
            warpView.startService()
            showResult("[Warp] startService 호출")
        }

        findViewById<Button>(R.id.btn_stop_service).setOnClickListener {
            if (!isWarpInitialized) return@setOnClickListener
            warpView.stopService()
            showResult("[Warp] stopService 호출")
        }

        findViewById<Button>(R.id.btn_venus_start).setOnClickListener {
            if (!isAuthCompleted || !isVenusInitialized) {
                showResult("먼저 Auth를 진행하세요")
                return@setOnClickListener
            }
            venusManager.startService()
        }

        findViewById<Button>(R.id.btn_venus_stop).setOnClickListener {
            if (!isAuthCompleted || !isVenusInitialized) {
                showResult("먼저 Auth를 진행하세요")
                return@setOnClickListener
            }
            venusManager.stopService()
            showResult("[Venus] stopService 호출")
        }

        findViewById<Button>(R.id.btn_jupiter_init).setOnClickListener {
            if (!isAuthCompleted) {
                showResult("먼저 Auth를 진행하세요")
                return@setOnClickListener
            }
            TJHanaLogger.i(logTag, "[JUPITER_INIT] initialize requested")
            jupiterManager.initialize()
            showResult("[Jupiter] initialize 호출")
        }

        findViewById<Button>(R.id.btn_jupiter_mock_toggle).setOnClickListener { button ->
            if (!isJupiterInitialized) {
                showResult("먼저 Jupiter Init을 진행하세요")
                return@setOnClickListener
            }
            isJupiterMockEnabled = !isJupiterMockEnabled
            jupiterManager.setMockMode(isJupiterMockEnabled)
            (button as Button).setText(
                if (isJupiterMockEnabled) R.string.jupiter_mock_on else R.string.jupiter_mock_off
            )
            showResult("[Jupiter] mock mode = $isJupiterMockEnabled")
        }

        findViewById<Button>(R.id.btn_jupiter_start).setOnClickListener {
            if (!isAuthCompleted || !isJupiterInitialized) {
                showResult("먼저 Auth/Jupiter Init을 진행하세요")
                return@setOnClickListener
            }
            jupiterManager.startService(UserMode.MODE_PEDESTRIAN)
            showResult("[Jupiter] startService 호출")
        }

        findViewById<Button>(R.id.btn_jupiter_stop).setOnClickListener {
            if (!isAuthCompleted || !isJupiterInitialized) {
                showResult("먼저 Auth/Jupiter Init을 진행하세요")
                return@setOnClickListener
            }
            jupiterManager.stopService { success, message ->
                showResult("[Jupiter] stopService: success=$success, message=$message")
            }
        }

        findViewById<Button>(R.id.btn_jupiter_destination).setOnClickListener {
            if (!isAuthCompleted || !isJupiterInitialized) {
                showResult("먼저 Auth/Jupiter Init을 진행하세요")
                return@setOnClickListener
            }
            val destination = Point(level_id = 1, x = 10, y = 10)
            jupiterManager.setNavigationDestination(destination)
            showResult("[Jupiter] destination 설정: $destination")
        }

        findViewById<Button>(R.id.btn_jupiter_routing).setOnClickListener {
            if (!isAuthCompleted || !isJupiterInitialized) {
                showResult("먼저 Auth/Jupiter Init을 진행하세요")
                return@setOnClickListener
            }
            val start = RoutingStart(level_id = 1, x = 0, y = 0, absolute_heading = 0)
            val destination = Point(level_id = 1, x = 10, y = 10)
            jupiterManager.requestRouting(start, destination, emptyList(), RequestType.INIT, false) { result ->
                val summary = result.routes.joinToString("\n") {
                    "level=${it.level_name}(${it.level_id}) (${it.x}, ${it.y})"
                }
                showResult(
                    "[Jupiter] routing 결과 ($start -> $destination)\n" +
                        "failureReason=${result.failureReason?.value ?: "nil"}\n" +
                        summary
                )
            }
        }
    }

    private fun buildWarpClickText(wards: List<WarpWard>): String {
        if (wards.isEmpty()) return "[Warp] 클릭 결과: count=0"

        val wardDetails = wards.joinToString("\n\n") { ward ->
            val urls = ward.ward_contents.map { it.contents_url.toString() }.distinct()
            buildString {
                append("ward id=${ward.id}, name=${ward.ward_name}, rssi=${ward.ward_rssi}, x=${ward.x}, y=${ward.y}\n")
                append(
                    if (urls.isEmpty()) "  urls: - 없음"
                    else "  urls:\n" + urls.joinToString("\n") { "  - $it" }
                )
            }
        }

        return "[Warp] 클릭 결과: count=${wards.size}\n$wardDetails"
    }

    private fun buildWarpSelectionText(wards: List<WarpWard>): String {
        if (wards.isEmpty()) return "[Warp] selection changed: count=0"

        val summary = wards.joinToString("\n") {
            "id=${it.id}, name=${it.ward_name}, rssi=${it.ward_rssi}, x=${it.x}, y=${it.y}"
        }
        return "[Warp] selection changed: count=${wards.size}\n$summary"
    }

    private fun showResult(message: String) {
        resultTextView.text = message
    }

    private fun showInitStatus() {
        resultTextView.text = listOf(authStatusText, warpInitStatusText, venusInitStatusText, jupiterInitStatusText)
            .joinToString("\n")
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
        }
    }

}
