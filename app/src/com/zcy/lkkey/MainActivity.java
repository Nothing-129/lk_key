package com.zcy.lkkey;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.os.*;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private BleKey key;
    private TextView status;
    private Button open, close;
    private final java.util.List<Button> extraControls=new java.util.ArrayList<>();
    private AlertDialog washDialog;
    private TextView washStatus;
    private Button washStart,washEnd,washSave,washStop,washRefresh;
    private Switch washAutoExit;
    private Calendar washFrom,washUntil;
    private boolean washLoaded;
    private Runnable pendingAction;

    private final BleKey.Listener listener=new BleKey.Listener(){
        public void changed(String message,boolean available){if(available&&pendingAction!=null){Runnable action=pendingAction;pendingAction=null;action.run();return;}status.setText("已连接，可以操作".equals(message)?"● 已连接":message);enable(available);if(washDialog!=null){washStatus.setText(message);enableWash(available);}}
        public void result(int opcode){if(opcode==10&&washDialog!=null)displayWash();}
        public void failed(String message){pendingAction=null;enable(false);if(washDialog!=null)enableWash(false);}
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarContrastEnforced(false);
        getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(true);
        scroll.setBackgroundColor(0xFF10151B); scroll.setVerticalScrollBarEnabled(false);
        FrameLayout canvas=new FrameLayout(this);scroll.addView(canvas,new ScrollView.LayoutParams(-1,-2));
        ImageView scene=new ImageView(this);scene.setImageResource(R.drawable.dusk_scene);scene.setScaleType(ImageView.ScaleType.FIT_XY);
        scene.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);canvas.addView(scene,new FrameLayout.LayoutParams(-1,dp(520)));
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0,dp(22),0,dp(16));canvas.addView(layout,new FrameLayout.LayoutParams(-1,-2));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            scroll.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(24),0,dp(24),0);header.setGravity(Gravity.TOP);
        LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);
        ImageView title=new ImageView(this);title.setImageResource(R.drawable.lynk_wordmark);title.setScaleType(ImageView.ScaleType.FIT_START);title.setPadding(0,0,0,dp(10));title.setContentDescription("领克 LYNK & CO");heading.addView(title,new LinearLayout.LayoutParams(-1,dp(32)));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        status=label("准备连接…",12,0xFFBBC5D0);status.setGravity(Gravity.END);status.setPadding(dp(10),dp(9),0,0);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);header.addView(status,new LinearLayout.LayoutParams(dp(144),-2));layout.addView(header);
        ImageView car=new ImageView(this);car.setImageResource(R.drawable.car_render);car.setScaleType(ImageView.ScaleType.FIT_CENTER);
        car.setPadding(dp(20),0,dp(20),0);car.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams carParams=new LinearLayout.LayoutParams(-1,dp(190));carParams.topMargin=dp(78);layout.addView(car,carParams);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);actions.setPadding(dp(4),dp(4),dp(4),dp(4));
        GradientDrawable dock=surface(0xD9272E37,22);dock.setStroke(dp(1),0x304F5C6D);actions.setBackground(dock);
        close=button("关锁",R.drawable.ic_lock);open=button("开锁",R.drawable.ic_unlock);
        actions.addView(close,new LinearLayout.LayoutParams(0,dp(60),1));
        View divider=new View(this);divider.setBackgroundColor(0x405F6A77);actions.addView(divider,new LinearLayout.LayoutParams(dp(1),dp(24)));
        actions.addView(open,new LinearLayout.LayoutParams(0,dp(60),1));
        LinearLayout.LayoutParams actionsParams=new LinearLayout.LayoutParams(-1,-2);actionsParams.setMargins(dp(32),dp(40),dp(32),0);layout.addView(actions,actionsParams);
        LinearLayout sheet=new LinearLayout(this);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(dp(18),dp(10),dp(18),dp(20));sheet.setBackground(surface(0xF21D232B,28));
        View handle=new View(this);handle.setBackground(surface(0xFF555F6A,2));LinearLayout.LayoutParams handleParams=new LinearLayout.LayoutParams(dp(34),dp(3));handleParams.gravity=Gravity.CENTER_HORIZONTAL;sheet.addView(handle,handleParams);
        TextView more=label("更多控制",15,0xFFE0E5ED);more.setPadding(dp(4),dp(18),0,dp(14));sheet.addView(more);
        LinearLayout windows=new LinearLayout(this);addControl(windows,"开窗",R.drawable.ic_window_open,()->requestAction(()->key.windows(true)));addControl(windows,"关窗",R.drawable.ic_window_close,()->requestAction(()->key.windows(false)));sheet.addView(windows);
        LinearLayout other=new LinearLayout(this);addControl(other,"后备箱",R.drawable.ic_trunk,()->requestAction(()->key.trunk()));addControl(other,"洗车",R.drawable.ic_wash,()->openWash());
        LinearLayout.LayoutParams otherParams=new LinearLayout.LayoutParams(-1,-2);otherParams.topMargin=dp(8);sheet.addView(other,otherParams);
        LinearLayout.LayoutParams sheetParams=new LinearLayout.LayoutParams(-1,-2);sheetParams.topMargin=dp(52);layout.addView(sheet,sheetParams);
        setContentView(scroll);enable(false);
        getWindow().getInsetsController().setSystemBarsAppearance(0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);

        key=BleKey.get(this);
        open.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);requestAction(()->key.command(2));});
        close.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);requestAction(()->key.command(1));});
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},1);

    }
    private void requestAction(Runnable action){
        if(pendingAction!=null||key.operating())return;
        if(key.available()){action.run();return;}
        pendingAction=action;enable(false);if(washDialog!=null)enableWash(false);
        key.connect();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String text,int size,int color){TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(color);view.setFontFeatureSettings("kern");return view;}
    private Button button(String text,int icon) {
        Button b=new Button(this);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(0xFFF2F5FA);
        android.graphics.drawable.Drawable symbol=getDrawable(icon).mutate();symbol.setBounds(0,0,dp(24),dp(24));
        android.text.SpannableString content=new android.text.SpannableString("\uFFFC  "+text);
        content.setSpan(new android.text.style.ImageSpan(symbol,android.text.style.ImageSpan.ALIGN_CENTER),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setText(content);b.setGravity(Gravity.CENTER);b.setPadding(dp(8),0,dp(8),0);b.setFontFeatureSettings("kern");b.setContentDescription(text);
        b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x307E98B4),surface(Color.TRANSPARENT,18),null));b.setStateListAnimator(null);
        b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);return b;
    }
    private void addControl(LinearLayout row,String title,int icon,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setTextSize(15);b.setTextColor(0xFFE0E6EF);
        b.setGravity(Gravity.CENTER);b.setPadding(dp(18),0,dp(16),0);b.setCompoundDrawablesRelativeWithIntrinsicBounds(icon,0,0,0);b.setCompoundDrawablePadding(dp(8));
        b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x227E98B4),surface(0xFF282F38,14),null));b.setStateListAnimator(null);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(72),1);
        if(row.getChildCount()==0)params.rightMargin=dp(4);else params.leftMargin=dp(4);
        row.addView(b,params);extraControls.add(b);b.setOnClickListener(v->action.run());
    }
    private void openWash(){
        washLoaded=false;washFrom=Calendar.getInstance();washUntil=(Calendar)washFrom.clone();washUntil.add(Calendar.HOUR_OF_DAY,2);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(12),dp(24),dp(12));
        TextView notice=label("洗车模式运行时车辆会解锁，离车不会自动锁车。请保管好车内物品。",14,0xFF695727);panel.addView(notice);
        washStatus=label("正在读取…",13,0xFF657267);washStatus.setPadding(0,dp(14),0,dp(10));panel.addView(washStatus);
        washStart=new Button(this);washEnd=new Button(this);panel.addView(washStart);panel.addView(washEnd);updateWashTimes();
        washStart.setOnClickListener(v->pickTime(true));washEnd.setOnClickListener(v->pickTime(false));
        washAutoExit=new Switch(this);washAutoExit.setText("蓝牙重连后自动退出");washAutoExit.setTextSize(14);panel.addView(washAutoExit);
        TextView hint=label("蓝牙断开后再次连接，将自动退出洗车模式。",12,0xFF657267);panel.addView(hint);
        washSave=new Button(this);washSave.setText("保存洗车时段");panel.addView(washSave);
        washStop=new Button(this);washStop.setText("停止洗车模式");panel.addView(washStop);
        washRefresh=new Button(this);washRefresh.setText("重新读取");panel.addView(washRefresh);
        for(Button b:new Button[]{washStart,washEnd,washSave,washStop,washRefresh}){
            b.setAllCaps(false);b.setTextSize(14);b.setTextColor(b==washSave?0xFFFFFFFF:0xFF17241F);
            b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x245E795F),surface(b==washSave?0xFF17241F:0xFFE7EBE2,14),null));b.setStateListAnimator(null);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(48));params.topMargin=dp(8);b.setLayoutParams(params);
        }
        ScrollView content=new ScrollView(this);content.addView(panel);
        washDialog=new AlertDialog.Builder(this).setTitle("洗车模式").setView(content).setNegativeButton("关闭",null).create();
        washDialog.setOnDismissListener(d->{washDialog=null;washLoaded=false;pendingAction=null;enable(key.available());});washDialog.show();washDialog.getWindow().setBackgroundDrawable(surface(0xFFF5F6F1,24));enableWash(key.available());
        washRefresh.setOnClickListener(v->{washLoaded=false;requestAction(()->key.readWash());});
        washSave.setOnClickListener(v->{
            if(washUntil.getTimeInMillis()<=washFrom.getTimeInMillis()||washUntil.getTimeInMillis()<=System.currentTimeMillis()){
                washStatus.setText("结束时间须晚于开始时间和当前时间");return;
            }
            Calendar start=(Calendar)washFrom.clone(),end=(Calendar)washUntil.clone();boolean autoExit=washAutoExit.isChecked();
            requestAction(()->key.setWash(start,end,autoExit));
        });
        washStop.setOnClickListener(v->{boolean autoExit=washAutoExit.isChecked();requestAction(()->key.setWash(null,null,autoExit));});
        requestAction(()->key.readWash());
    }
    private void enableWash(boolean available){
        available=pendingAction==null&&!key.operating();
        for(Button b:new Button[]{washStart,washEnd,washSave,washStop})b.setAlpha(available&&washLoaded?1f:.48f);
        washStart.setEnabled(available&&washLoaded);washEnd.setEnabled(available&&washLoaded);
        washAutoExit.setEnabled(available&&washLoaded);washSave.setEnabled(available&&washLoaded);washStop.setEnabled(available&&washLoaded);
        washRefresh.setEnabled(available);
    }
    private void updateWashTimes(){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA);
        washStart.setText("开始  "+format.format(washFrom.getTime()));washEnd.setText("结束  "+format.format(washUntil.getTime()));
    }
    private void pickTime(boolean start){
        Calendar current=start?washFrom:washUntil;
        new DatePickerDialog(this,(picker,year,month,day)->{
            new TimePickerDialog(this,(clock,hour,minute)->{
                current.set(year,month,day,hour,minute,0);current.set(Calendar.MILLISECOND,0);updateWashTimes();
            },current.get(Calendar.HOUR_OF_DAY),current.get(Calendar.MINUTE),true).show();
        },current.get(Calendar.YEAR),current.get(Calendar.MONTH),current.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void displayWash(){
        WashMode mode=key.wash();if(mode==null)return;
        washLoaded=true;washAutoExit.setChecked(mode.autoExit);
        if(mode.start!=null&&mode.end!=null){washFrom=(Calendar)mode.start.clone();washUntil=(Calendar)mode.end.clone();}
        updateWashTimes();washStatus.setText(mode.active?"设备状态：洗车模式运行中":mode.start!=null&&mode.start.getTimeInMillis()>System.currentTimeMillis()?"设备状态：已预约洗车时段":"设备状态：洗车模式未运行");enableWash(key.available());
    }
    private void enable(boolean on) { on=key!=null&&pendingAction==null&&!key.operating();open.setEnabled(on);close.setEnabled(on);open.setAlpha(on?1f:.48f);close.setAlpha(on?1f:.48f);for(Button b:extraControls){b.setEnabled(on);b.setAlpha(on?1f:.48f);} }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(results.length==1&&results[0]==0)key.connect();else status.setText("需要附近设备权限，请重新打开应用授权");
    }
    @Override public void onStart(){super.onStart();key.attach(listener);if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED)key.connect();}
    @Override public void onStop(){pendingAction=null;key.detach(listener);super.onStop();}
}
