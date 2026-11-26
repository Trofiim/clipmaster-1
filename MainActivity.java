package com.clipmaster.ai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri selectedVideo;
    private static final int PICK_VIDEO = 1001;

    @Override 
    public void onSuccess(String message) {
    sendResult(message);
    }

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
                    WebView w,
                    ValueCallback<Uri[]> cb,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = cb;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                startActivityForResult(intent, PICK_VIDEO);
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private class VideoBridge {
        @JavascriptInterface
        public void trimVideo(final int seconds) {
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
                        public void onCompleted(ArrayList<String> clips) {
                            sendResult("Готово! Создано клипов: "
                                    + clips.size());
                        }

                        @Override
                        public void onError(String error) {
                            sendResult(error);
                        }
                    }
            );
        }
    }

    private void sendResult(final String message) {
        runOnUiThread(() -> {
            if (web != null) {
                String safe = message.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n");

                web.evaluateJavascript(
                        "if(window.onNativeTrimResult){" +
                        "window.onNativeTrimResult('" + safe + "');}",
                        null
                );
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode,
                                   int resultCode,
                                   Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO && fileCallback != null) {
            Uri[] result = null;

            if (resultCode == RESULT_OK && data != null
                    && data.getData() != null) {

                selectedVideo = data.getData();
                result = new Uri[]{selectedVideo};

                try {
                    getContentResolver().takePersistableUriPermission(
                            selectedVideo,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {}
            }

            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
