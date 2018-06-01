package io.predic.tracker;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class Pixel extends WebView {

    private RelativeLayout relativeLayout;
    private ViewGroup view;
    private Handler handler;
    private Runnable runnable;

    public Pixel(Context context){
        super(context);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                finishView();
            }
        };

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

        ViewGroup view = (ViewGroup) ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);

        relativeLayout = new RelativeLayout(context);
        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(1,1));
        relativeLayout.addView(this);
        this.view = view ;
        this.view.addView(relativeLayout);
    }
    public void shoot(final String url){
        postDelayed(new Runnable() {
            @Override
            public void run() {
                loadUrl(url);
            }
        }, 500);

        this.setVisibility(WebView.VISIBLE);
        try {
            handler.removeCallbacks(runnable);
        }
        catch (Exception e){ }
        handler.postDelayed(runnable,10 * 1000);

        Log.d("PREDICIO", url + " - worked");
    }
    private void finishView(){
        try {
            if (relativeLayout != null) {
                relativeLayout.removeAllViews();
                if (view != null) {
                    view.removeView(relativeLayout);
                    view.invalidate();
                    loadUrl("about:blank");
                }
            }
        }
        catch(Exception e){  Log.e("PREDICIO","finishView error:" + e.toString()); }
    }
}


