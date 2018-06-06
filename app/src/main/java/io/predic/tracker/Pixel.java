package io.predic.tracker;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

class Pixel extends WebView {

    private RelativeLayout relativeLayout;
    private ViewGroup view;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            finishView();
        }
    };

    public Pixel(Activity activity){
        super(activity);
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());

        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

        ViewGroup view = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);

        relativeLayout = new RelativeLayout(activity);
        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(1,1));
        relativeLayout.addView(this);
        this.view = view ;
        this.view.addView(relativeLayout);
    }
    void shoot(String url){
        loadUrl(url);
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

