package com.ballsort.game;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.ads.*;

public class MainActivity extends AppCompatActivity {

    // ╔══════════════════════════════════════════╗
    // ║  👇 APNE META PLACEMENT IDs YAHAN DAALO  ║
    // ╠══════════════════════════════════════════╣
    private static final String BANNER_ID       = "YOUR_BANNER_PLACEMENT_ID";
    private static final String INTERSTITIAL_ID = "YOUR_INTERSTITIAL_PLACEMENT_ID";
    // ╚══════════════════════════════════════════╝

    private WebView webView;
    private AdView  bannerAd;
    private InterstitialAd interAd;
    private boolean interReady = false;

    @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudienceNetworkAds.initialize(this);

        // Layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A1628);

        webView = new WebView(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, 0, 1f);
        webView.setLayoutParams(wp);

        FrameLayout bannerWrap = new FrameLayout(this);
        bannerWrap.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        bannerWrap.setBackgroundColor(0xFFFFFFFF);

        root.addView(webView);
        root.addView(bannerWrap);
        setContentView(root);

        // WebView
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        webView.setWebChromeClient(new WebChromeClient());

        // JS Bridge — game HTML se ad calls aayenge
        webView.addJavascriptInterface(new AdBridge(), "AdBridge");
        webView.loadUrl("file:///android_asset/game.html");

        loadBanner(bannerWrap);
        preloadInter();
    }

    // ── Banner ──────────────────────────────
    private void loadBanner(FrameLayout wrap) {
        bannerAd = new AdView(this, BANNER_ID, AdSize.BANNER_HEIGHT_50);
        wrap.addView(bannerAd);
        bannerAd.loadAd(bannerAd.buildLoadAdConfig().withAdListener(new AdListener() {
            @Override public void onError(Ad a, AdError e) {}
            @Override public void onAdLoaded(Ad a) {}
            @Override public void onAdClicked(Ad a) {}
            @Override public void onLoggingImpression(Ad a) {}
        }).build());
    }

    // ── Interstitial ────────────────────────
    private void preloadInter() {
        interAd = new InterstitialAd(this, INTERSTITIAL_ID);
        interAd.loadAd(interAd.buildLoadAdConfig().withAdListener(new InterstitialAdListener() {
            @Override public void onInterstitialDisplayed(Ad a) {}
            @Override public void onInterstitialDismissed(Ad a) {
                // Ad dismiss → next level signal
                runOnUiThread(()-> webView.evaluateJavascript("javascript:nextLevel()", null));
                preloadInter();
            }
            @Override public void onError(Ad a, AdError e) { interReady = false; }
            @Override public void onAdLoaded(Ad a) { interReady = true; }
            @Override public void onAdClicked(Ad a) {}
            @Override public void onLoggingImpression(Ad a) {}
        }).build());
    }

    // ── JS Bridge ───────────────────────────
    class AdBridge {
        @JavascriptInterface
        public void showInterstitial() {
            runOnUiThread(() -> {
                if (interAd != null && interAd.isAdLoaded() && interReady) {
                    interAd.show();
                } else {
                    // Ad nahi mila — seedha next level
                    webView.evaluateJavascript("javascript:nextLevel()", null);
                }
            });
        }
    }

    @Override protected void onDestroy() {
        if (bannerAd != null) bannerAd.destroy();
        if (interAd  != null) interAd.destroy();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
