package com.peter.admonitor.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.peter.admonitor.R;

public class AdLandingActivity extends AppCompatActivity {

    public static final String URL = "url";
    WebView wv;
    ProgressBar pb;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_landing);
        wv = findViewById(R.id.wv);
        pb = findViewById(R.id.pb);
        WebSettings settings = wv.getSettings();
        // 能够的调用JavaScript代码, 设置加载进来的页面自适应手机屏幕,并且WebView双击变大，再双击后变小，当手动放大后，双击可以恢复到原始大小
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setGeolocationEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setTextZoom(100);

        wv.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pb.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("http:") && !url.startsWith("https:")) {
                    //只要不是这两个开头的，都尝试跳转，如：mailto:、geo:、tel:、sms:、alipays:等
                    try {
                        Log.d("webview jump", "init url=" + AdLandingActivity.this.url + "\noverrideUrl=" + url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {//如果没有找到对应的Activity，则会抛ActivityNotFoundException
                        Log.e("webview jump", "Try To Override Url: Unknown Url Scheme. Target Url:\n" + url, e);
                    }
                    return true;//在任何情况下都返回true, 这是为了避免展示网络错误页面
                }
                view.loadUrl(url);
                return true;
            }
        });
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                setTitle(title);
            }
        });

        url = getIntent().getStringExtra(URL);
        if (!TextUtils.isEmpty(url)) {
            wv.loadUrl(url);
        }
    }
}
