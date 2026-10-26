package ru.stuzhin.tower;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.my.target.ads.MyTargetView;
import com.my.target.common.models.IAdLoadingError;

public class MainActivity extends Activity {
    private WebView web;
    private LinearLayout root;
    private FrameLayout adContainer;
    private MyTargetView bannerAd;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            try { lp.getClass().getField("layoutInDisplayCutoutMode").setInt(lp, 1); }
            catch (Exception ignored) { }
            w.setAttributes(lp);
        }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#12081f"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#12081f"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        adContainer = new FrameLayout(this);
        adContainer.setBackgroundColor(Color.parseColor("#12081f"));
        int adHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        root.addView(adContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, adHeight));

        setContentView(root);
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");

        int slotId = getResources().getInteger(R.integer.vk_ads_slot_id);
        if (slotId > 0) setupBanner(slotId);
        immersive();
    }

    private void setupBanner(int slotId) {
        bannerAd = new MyTargetView(this);
        bannerAd.setSlotId(slotId);
        bannerAd.setAdSize(MyTargetView.AdSize.ADSIZE_320x50);
        bannerAd.setListener(new MyTargetView.MyTargetViewListener() {
            @Override
            public void onLoad(MyTargetView view) {
                runOnUiThread(() -> {
                    if (view.getParent() instanceof android.view.ViewGroup) {
                        ((android.view.ViewGroup) view.getParent()).removeView(view);
                    }
                    adContainer.removeAllViews();
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            android.view.Gravity.CENTER);
                    adContainer.addView(view, lp);
                });
            }

            @Override
            public void onNoAd(IAdLoadingError error, MyTargetView view) {
                android.util.Log.w("TowerAds", "Banner failed to load: " +
                        (error == null ? "unknown error" : error.getMessage()));
            }

            @Override public void onClick(MyTargetView view) { }
            @Override public void onShow(MyTargetView view) { }
        });
        bannerAd.load();
    }

    private void immersive() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        root.setSystemUiVisibility(flags);
        if (web != null) web.setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override
    public void onBackPressed() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript("(window.onAndroidBack ? window.onAndroidBack() : 'exit')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if (v == null || v.contains("exit")) finish();
            }
        });
    }

    @Override
    protected void onPause() {
        if (web != null) {
            web.evaluateJavascript("window.onAndroidPause && window.onAndroidPause()", null);
            web.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.evaluateJavascript("window.onAndroidResume && window.onAndroidResume()", null);
        }
        immersive();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (bannerAd != null) {
            bannerAd.setListener(null);
            bannerAd.destroy();
            bannerAd = null;
        }
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
