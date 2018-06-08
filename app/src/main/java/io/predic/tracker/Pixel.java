package io.predic.tracker;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class Pixel extends WebView {

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            loadUrl("about:blank");
        }
    };

    public Pixel(Activity activity){
        super(activity);
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());
        setWillNotDraw(true);
        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);
    }
    void shoot(String url){
        try {
            handler.removeCallbacks(runnable);
        }
        catch (Exception e){ }

        loadUrl(url);

        handler.postDelayed(runnable,10 * 1000);

        Log.d("PREDICIO", url + " - worked");
    }
}

