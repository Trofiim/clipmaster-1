package com.clipmaster.ai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri selectedVideo;
    private static final int PICK_VIDEO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new VideoBridge(), "AndroidClipper");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                fileCallback = callback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(intent, PICK_VIDEO);
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO) {
            if (data != null && data.getData() != null) {
                selectedVideo = data.getData();

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(
                            new Uri[]{selectedVideo});
                    fileCallback = null;
                }
            } else {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                }
            }
        }
    }

    public class VideoBridge {

        @JavascriptInterface
        public void pickVideo() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, PICK_VIDEO);
            });
        }

        @JavascriptInterface
        public void trimVideo(int seconds) {
            runOnUiThread(() -> {
                if (selectedVideo == null) {
                    sendResult("Сначала выберите видео.");
                    return;
                }

                VideoTrimmer.trim(
                        MainActivity.this,
                        selectedVideo,
                        seconds,
                        new VideoTrimmer.Callback() {
                            @Override
                            public void onSuccess(String message) {
                                sendResult(message);
                            }

                            @Override
                            public void onError(String message) {
                                sendResult(message);
                            }
                        }
                );
            });
        }
    }

    private void sendResult(String message) {
        web.post(() -> web.evaluateJavascript(
                "window.onClipResult(" +
                JSONObjectQuote(message) + ");",
                null
        ));
    }

    private String JSONObjectQuote(String text) {
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}
