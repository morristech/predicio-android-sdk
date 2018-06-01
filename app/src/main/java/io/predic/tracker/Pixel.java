package io.predic.tracker;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class Pixel extends WebView {

    private RelativeLayout relativeLayout;
    private ViewGroup view;

    public Pixel(Context context){
        super(context);
        ViewGroup view = (ViewGroup) ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        init(context,view);
    }
    private void init(Context context,ViewGroup view){

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());
        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

        try {
            relativeLayout = new RelativeLayout(context);
            relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(1000,1000));
            relativeLayout.addView(this);
            this.view = view ;
            this.view.addView(relativeLayout);
        }
        catch(Exception ex){ }
    }
    public void shoot(String url){
        loadUrl(url);
        this.setVisibility(WebView.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finishView();
            }
        },10 * 1000);
    }
    private  void  finishView(){
        try {
            if (relativeLayout != null) {
                relativeLayout.removeAllViews();
                if (view != null) {
                    Log.d("Predicio", "invalidate");
                    view.removeView(relativeLayout);
                    view.invalidate();
                }
            }
        }
        catch(Exception ex){ }
    }
}


