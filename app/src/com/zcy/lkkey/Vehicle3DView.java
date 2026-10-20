package com.zcy.lkkey;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.webkit.*;
import android.widget.FrameLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

/** Offline WebGL view. Android sends visual feedback; web content cannot send commands. */
public final class Vehicle3DView extends FrameLayout {
    private static final String HOST="appassets.androidplatform.net";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final VehicleVisualState state=new VehicleVisualState();
    private WebView web;
    private boolean ready,active,destroyed,resetAvailable;
    private int probes;
    private java.util.function.Consumer<Boolean> readyListener=value->{};
    private java.util.function.Consumer<Boolean> resetListener=value->{};

    public Vehicle3DView(Context context) {
        super(context);
        setContentDescription("3D 车辆展示，拖动环视，双指缩放；动画仅表示指令反馈");
        try {
            web=new WebView(context);
            web.setBackgroundColor(Color.TRANSPARENT);
            web.setVisibility(INVISIBLE);
            web.setOverScrollMode(OVER_SCROLL_NEVER);
            WebSettings settings=web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
            settings.setBlockNetworkLoads(true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setSupportMultipleWindows(false);
            web.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
                @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                    Uri uri=request.getUrl();String path=uri.getPath();
                    if(!"https".equals(uri.getScheme())||!HOST.equals(uri.getHost())||path==null||path.contains(".."))return missing();
                    String mime=path.endsWith(".html")?"text/html":path.endsWith(".js")?"application/javascript":
                        path.endsWith(".wasm")?"application/wasm":path.endsWith(".css")?"text/css":
                        path.endsWith(".json")?"application/json":"application/octet-stream";
                    try {
                        return new WebResourceResponse(mime,"UTF-8",200,"OK",Collections.singletonMap("Cache-Control","no-cache"),
                            getContext().getAssets().open("vehicle3d"+path));
                    } catch(IOException ignored){return missing();}
                }
                @Override public void onPageFinished(WebView view,String url) {handler.removeCallbacks(probe);probes=0;handler.post(probe);}
                @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail) {
                    unavailable();removeView(view);view.destroy();web=null;return true;
                }
            });
            web.setOnTouchListener((view,event)->{
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN)getParent().requestDisallowInterceptTouchEvent(true);
                if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            });
            addView(web,new LayoutParams(-1,-1));
            boolean reduce=Settings.Global.getFloat(context.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;
            web.loadUrl("https://"+HOST+"/android.html?reduce="+(reduce?"1":"0"));
        } catch(RuntimeException unsupported){unavailable();}
    }
    private static WebResourceResponse missing(){return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    public void setReadyListener(java.util.function.Consumer<Boolean> listener){readyListener=listener;listener.accept(ready);}
    public void setResetListener(java.util.function.Consumer<Boolean> listener){resetListener=listener;listener.accept(resetAvailable);}
    /** Keep WebGL's viewport stable while the control drawer uses the empty foreground. */
    public void setSceneHeight(int height){
        if(web==null||height<=0)return;
        LayoutParams params=(LayoutParams)web.getLayoutParams();
        if(params.height!=height){params.height=height;web.setLayoutParams(params);}
    }
    private void updateResetAvailable(boolean value){if(resetAvailable!=value){resetAvailable=value;resetListener.accept(value);}}
    private final Runnable probe=new Runnable(){public void run(){
        if(destroyed||web==null)return;
        web.evaluateJavascript("window.vehicle3D ? (window.vehicle3D.error ? -1 : window.vehicle3D.ready ? (window.vehicle3D.needsReset() ? 2 : 1) : 0) : 0",value->{
            if(destroyed||web==null)return;
            if("1".equals(value)||"2".equals(value)) {
                if(!ready){ready=true;web.setVisibility(VISIBLE);readyListener.accept(true);publish();web.evaluateJavascript("window.vehicle3D.setActive("+active+")",null);}
                // Read camera state only while visible; also detect context loss.
                updateResetAvailable("2".equals(value));
                if(active)handler.postDelayed(probe,250);
            } else if("-1".equals(value)||++probes>150)unavailable();
            else handler.postDelayed(probe,200);
        });
    }};
    private void unavailable(){ready=false;updateResetAvailable(false);handler.removeCallbacks(probe);if(web!=null)web.setVisibility(INVISIBLE);readyListener.accept(false);}
    public void resetView(){
        if(ready&&web!=null&&!destroyed)web.evaluateJavascript("if(window.vehicle3D && window.vehicle3D.ready) window.vehicle3D.reset()",null);
    }
    public void acknowledge(String action){if(state.acknowledge(action))publish();}
    private void publish(){if(ready&&web!=null)web.evaluateJavascript("window.vehicle3D.apply("+state.json()+")",null);}
    public void setActive(boolean value) {
        active=value;
        if(web==null||destroyed)return;
        if(value){web.onResume();handler.removeCallbacks(probe);handler.post(probe);}
        if(ready)web.evaluateJavascript("window.vehicle3D.setActive("+value+")",null);
        if(!value){web.onPause();handler.removeCallbacks(probe);}
    }
    public void destroy(){destroyed=true;handler.removeCallbacksAndMessages(null);if(web!=null){removeView(web);web.destroy();web=null;}}
}
