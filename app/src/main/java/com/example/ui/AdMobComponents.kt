package com.example.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PinGoldDark
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File

object AdMobTestAdHelper {
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    @Volatile
    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    var showSimulatedInterstitial by mutableStateOf(false)
        private set

    /**
     * Checks whether the device has a valid DRM render node (/dev/dri/renderD128) and is not a
     * headless cloud emulator where Chromium WebView triggers MESA rendernode errors.
     */
    fun canUseHardwareWebViewAds(): Boolean {
        val hasRenderNode = try {
            File("/dev/dri/renderD128").exists()
        } catch (_: Throwable) {
            false
        }
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("cuttlefish") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.contains("vbox86p")
        return hasRenderNode && !isEmulator
    }

    fun initializeSdk(context: Context) {
        if (!canUseHardwareWebViewAds()) {
            // Skip spawning Chromium WebView process on virtualized emulators without DRM rendernode
            isInitialized = true
            return
        }
        if (isInitialized) return
        try {
            MobileAds.initialize(context.applicationContext) {
                isInitialized = true
                loadInterstitialAd(context.applicationContext)
            }
        } catch (_: Throwable) {
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (!canUseHardwareWebViewAds()) return
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context.applicationContext,
                TEST_INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                    }
                }
            )
        } catch (_: Throwable) {
            interstitialAd = null
            isLoadingInterstitial = false
        }
    }

    fun showInterstitialAdIfAvailable(activity: Activity, onAdDismissedOrUnavailable: () -> Unit = {}) {
        if (!canUseHardwareWebViewAds()) {
            showSimulatedInterstitial = true
            onAdDismissedOrUnavailable()
            return
        }
        val ad = interstitialAd
        if (ad != null && !activity.isFinishing && !activity.isDestroyed) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissedOrUnavailable()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissedOrUnavailable()
                }
            }
            try {
                ad.show(activity)
            } catch (_: Throwable) {
                interstitialAd = null
                onAdDismissedOrUnavailable()
            }
        } else {
            loadInterstitialAd(activity)
            onAdDismissedOrUnavailable()
        }
    }

    fun dismissSimulatedInterstitial() {
        showSimulatedInterstitial = false
    }
}

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobTestAdHelper.TEST_BANNER_AD_UNIT_ID
) {
    val useWebViewAd = remember { AdMobTestAdHelper.canUseHardwareWebViewAds() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("admob_banner_container")
    ) {
        if (useWebViewAd) {
            val context = LocalContext.current
            val adView = remember(adUnitId) {
                AdView(context).apply {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    try {
                        loadAd(AdRequest.Builder().build())
                    } catch (_: Throwable) {
                    }
                }
            }

            DisposableEffect(adView) {
                onDispose {
                    try {
                        adView.destroy()
                    } catch (_: Throwable) {
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admob_banner_view"),
                    factory = { adView }
                )
            }
        } else {
            // Native Compose Test Ad Banner (avoids Chromium MESA rendernode errors on cloud emulators)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("admob_banner_view"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, PinGoldDark.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PinGoldDark)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ad",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Column {
                                Text(
                                    text = "Google AdMob Test Banner",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = adUnitId,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            text = "Test Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PinGoldDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdMobTestInterstitialOverlay(
    adUnitId: String = AdMobTestAdHelper.TEST_INTERSTITIAL_AD_UNIT_ID
) {
    if (!AdMobTestAdHelper.showSimulatedInterstitial) return

    Dialog(
        onDismissRequest = { AdMobTestAdHelper.dismissSimulatedInterstitial() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF0F172A).copy(alpha = 0.96f),
            modifier = Modifier
                .fillMaxSize()
                .testTag("admob_interstitial_dialog")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                IconButton(
                    onClick = { AdMobTestAdHelper.dismissSimulatedInterstitial() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .testTag("close_interstitial_ad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Test Ad",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PinGoldDark)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Google AdMob • Test Interstitial Ad",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Nice job! This is a full-screen test ad.",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Text(
                        text = "Ad Unit ID: $adUnitId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { AdMobTestAdHelper.dismissSimulatedInterstitial() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinGoldDark,
                            contentColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.testTag("dismiss_interstitial_button")
                    ) {
                        Text(
                            text = "Close Ad",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
