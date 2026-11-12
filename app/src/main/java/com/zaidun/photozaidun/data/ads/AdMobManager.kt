package com.zaidun.photozaidun.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber

object AdMobManager {

    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4183656778309205/5748993923"

    // DAFTAR EMAIL ADMIN / PEMILIK (BEBAS IKLAN 100%)
    private val ADMIN_EMAILS = setOf(
        "zdnsyh10@gmail.com",
        "zid4nsyhid@gmail.com",
        "zidansyahidagrifasa@gmail.com"
    )

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {
                Timber.tag("AdMob").d("AdMob MobileAds SDK initialized successfully.")
                loadInterstitialAd(context)
            }
        } catch (e: Exception) {
            Timber.tag("AdMob").e(e, "Gagal menginisialisasi AdMob SDK")
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isLoadingAd) return
        isLoadingAd = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingAd = false
                    Timber.tag("AdMob").d("Iklan Interstitial berhasil di-load!")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingAd = false
                    Timber.tag("AdMob").e("Gagal load Iklan Interstitial: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAdBeforeExport(
        activity: Activity,
        userEmail: String = "",
        onAdFinished: () -> Unit
    ) {
        // BYPASS OTOMATIS JIKA EMAIL ADALAH EMAIL ADMIN / PEMILIK
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail.isNotBlank() && ADMIN_EMAILS.any { it.lowercase() == cleanEmail }) {
            Timber.tag("AdMob").d("Bypassing AdMob ad for Admin/Owner: $cleanEmail")
            onAdFinished()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdFinished()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdFinished()
                }
            }
            ad.show(activity)
        } else {
            // Jika iklan belum siap/gagal load, langsung ekspor tanpa menghambat fotografer
            loadInterstitialAd(activity)
            onAdFinished()
        }
    }
}
