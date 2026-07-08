package com.tjlabs.tjhana_demo_android

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tjlabs.tjhana_sdk_android.TJHanaAuth
import com.tjlabs.tjhana_sdk_android.TJVenusManager
import com.tjlabs.tjhana_sdk_android.TJVenusManagerDelegate
import com.tjlabs.tjhana_sdk_android.VenusInitErrorCode
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HanaAuthInitTest {

    @Test
    fun verifyAuthAndInit() {
        val args = InstrumentationRegistry.getArguments()
        val sectorId = args.getString("sectorId")?.toIntOrNull() ?: DEFAULT_SECTOR_ID
        val userId = args.getString("userId") ?: DEFAULT_USER_ID
        val label = "userId=$userId, sectorId=$sectorId"

        val accessKey = BuildConfig.AUTH_ACCESS_KEY
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY
        assertTrue("AUTH_ACCESS_KEY missing", accessKey.isNotBlank())
        assertTrue("AUTH_SECRET_ACCESS_KEY missing", accessSecretKey.isNotBlank())

        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application

        val authLatch = CountDownLatch(1)
        var authSuccess = false
        var authCode = -1
        TJHanaAuth.auth(application, accessKey, accessSecretKey) { code, success ->
            authCode = code
            authSuccess = success
            authLatch.countDown()
        }
        assertTrue("auth callback timeout ($label)", authLatch.await(AUTH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue("auth failed ($label, code=$authCode)", authSuccess)

        val initLatch = CountDownLatch(1)
        var initSuccess = false
        var initErrorCode: VenusInitErrorCode? = null
        val venusManager = TJVenusManager(application).apply {
            delegate = object : TJVenusManagerDelegate {
                override fun onInitSuccess(isSuccess: Boolean, code: VenusInitErrorCode?) {
                    initSuccess = isSuccess
                    initErrorCode = code
                    initLatch.countDown()
                }
            }
        }
        venusManager.initialize(id = userId, sector_id = sectorId)
        assertTrue("init callback timeout ($label)", initLatch.await(INIT_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue("init failed ($label, errorCode=$initErrorCode)", initSuccess)
        assertNull("init returned errorCode ($label, errorCode=$initErrorCode)", initErrorCode)
    }

    companion object {
        private const val DEFAULT_USER_ID = "ci_verify_user_android"
        private const val DEFAULT_SECTOR_ID = 8
        private const val AUTH_TIMEOUT_SEC = 60L
        private const val INIT_TIMEOUT_SEC = 120L
    }
}
