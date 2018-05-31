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
    private Context ctx;
    private final static String url ="https://www.mobilesiteserver.com/display/?tag=jx6ako";
    private  String get;
    public int pixelTime  = 10;

    public Pixel(Context context,String get){
        super(context);
        ViewGroup view = (ViewGroup) ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        init(context,view,get);
    }

    public Pixel(Context context, ViewGroup view,String get) {
        super(context);
        init(context,view,get);

    }

    private void init(Context ctx,ViewGroup view,String get){
        this.ctx = ctx;
        this.get = get!=null ? get : "";
        setSetting();
        try {
            rl = new RelativeLayout(ctx);
            rl.setLayoutParams(new RelativeLayout.LayoutParams(1,1));
            rl.addView(this);
            this.view = view ;
            this.view.addView(rl);
        }catch(Exception ex){}
    }

    private void setSetting(){
        setWebChromeClient(new WebChromeClient());
        setWebViewClient(new WebViewClient());
        setOnTouchListener(null);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);

    }

    public void shoot(){
        loadUrl(url+get);
        this.setVisibility(WebView.VISIBLE);
        trigger(pixelTime<5?5:pixelTime);
    }

    private void trigger(int sec){
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                finishView();
            }

        },sec * 1000);
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
        }catch(Exception ex){
            return;
        }
    }
}


