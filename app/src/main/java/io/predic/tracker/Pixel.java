package io.predic.tracker;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class Pixel extends WebView {

    private RelativeLayout rl;
    private ViewGroup view;

    public Pixel(Context context){
        super(context);
        ViewGroup view = (ViewGroup) ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        init(context,view);
    }
    public Pixel(Context context, ViewGroup view) {
        super(context);
        init(context,view);
    }

    private void init(Context ctx,ViewGroup view){

        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());
        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

        try {
            rl = new RelativeLayout(ctx);
            rl.setLayoutParams(new RelativeLayout.LayoutParams(1,1));
            rl.addView(this);
            this.view = view ;
            this.view.addView(rl);
        }
        catch(Exception ex){}
    }

    public void shoot(String AAID){
        loadUrl("http://ws.predic.io/pixel?aaid=" + AAID);
        this.setVisibility(WebView.VISIBLE);
        trigger();
    }

    private void trigger(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finishView();
            }
        },10 * 1000);
    }

    private  void  finishView(){
        try {
            if (rl != null) {
                rl.removeAllViews();
                if (view != null) {
                    view.removeView(rl);
                    view.invalidate();
                }
            }
        }
        catch(Exception ex){ }
    }
}


