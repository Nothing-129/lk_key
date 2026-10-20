package com.zcy.lkkey;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.os.*;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private BleKey key;
    private TextView status;
    private TextView lastCommand;
    private String connectionMessage="准备连接…";
    private Button open, close;
    private DuskEffectsView sceneEffects;
    private Vehicle3DView vehicle3D;
    private final java.util.List<Button> extraControls=new java.util.ArrayList<>();
    private AlertDialog washDialog;
    private TextView washStatus;
    private Button washStart,washEnd,washSave,washStop,washRefresh;
    private Switch washAutoExit;
    private Calendar washFrom,washUntil;
    private boolean washLoaded;
    private Runnable pendingAction;

    private final BleKey.Listener listener=new BleKey.Listener(){
        public void changed(String message,boolean available){if(available&&pendingAction!=null){Runnable action=pendingAction;pendingAction=null;action.run();return;}updateStatus(message);enable(available);if(washDialog!=null){washStatus.setText(message);enableWash(available);}}
        public void result(int opcode){updateLastCommand();if(vehicle3D!=null)vehicle3D.acknowledge(key.acknowledgedAction());if(opcode==10&&washDialog!=null)displayWash();}
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
        VehicleSceneView scene=new VehicleSceneView(this);canvas.addView(scene,new FrameLayout.LayoutParams(-1,dp(520)));
        sceneEffects=new DuskEffectsView(this);scene.addView(sceneEffects,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0,dp(22),0,0);canvas.addView(layout,new FrameLayout.LayoutParams(-1,-2));
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layout.setMinimumHeight(Math.max(0,b-t-scroll.getPaddingTop()-scroll.getPaddingBottom())));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            scroll.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            layout.setMinimumHeight(Math.max(0,scroll.getHeight()-safe.top-safe.bottom));return insets;
        });
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(24),0,dp(24),0);header.setGravity(Gravity.TOP);
        LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);
        ImageView title=new ImageView(this);title.setImageResource(R.drawable.lynk_wordmark);title.setScaleType(ImageView.ScaleType.FIT_START);title.setPadding(0,0,0,dp(10));title.setContentDescription("领克 LYNK & CO");heading.addView(title,new LinearLayout.LayoutParams(-1,dp(32)));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        status=label("准备连接…",12,0xFFBBC5D0);status.setGravity(Gravity.END);status.setPadding(dp(10),dp(9),0,0);
        status.setSingleLine(true);status.setEllipsize(TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setOnClickListener(v->Toast.makeText(this,connectionMessage,Toast.LENGTH_LONG).show());updateStatus(connectionMessage);
        int headerHeight=Math.max(dp(32),status.getLineHeight()+status.getPaddingTop()+status.getPaddingBottom());
        header.addView(status,new LinearLayout.LayoutParams(dp(144),-1));layout.addView(header,new LinearLayout.LayoutParams(-1,headerHeight));
        vehicle3D=new Vehicle3DView(this);View carSlot=vehicle3D;
        ImageButton resetView=new ImageButton(this);resetView.setImageResource(R.drawable.ic_view_reset);
        resetView.setContentDescription("复位车辆视角");resetView.setTooltipText("复位视角");resetView.setPadding(dp(12),dp(12),dp(12),dp(12));
        resetView.setBackgroundTintList(null);resetView.setBackground(new RippleDrawable(ColorStateList.valueOf(0x307E98B4),surface(0x90272E37,24),null));resetView.setStateListAnimator(null);
        FrameLayout.LayoutParams resetParams=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.BOTTOM|Gravity.END);resetParams.setMarginEnd(dp(24));resetParams.bottomMargin=dp(12);
        resetView.setVisibility(View.GONE);vehicle3D.addView(resetView,resetParams);
        vehicle3D.setResetListener(visible->resetView.setVisibility(visible?View.VISIBLE:View.GONE));
        resetView.setOnClickListener(v->vehicle3D.resetView());
        vehicle3D.setReadyListener(ready->{scene.setThreeDActive(ready);resetView.setEnabled(ready);resetView.setAlpha(ready?1f:.4f);});
        // The drawer may consume foreground space, while the scene keeps its viewport.
        LinearLayout.LayoutParams carParams=new LinearLayout.LayoutParams(-1,dp(230),1);carParams.topMargin=dp(12);layout.addView(carSlot,carParams);
        lastCommand=label("",11,0xFFBBC5D0);lastCommand.setGravity(Gravity.CENTER);lastCommand.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams recordParams=new LinearLayout.LayoutParams(-1,-2);recordParams.setMargins(dp(20),dp(8),dp(20),0);layout.addView(lastCommand,recordParams);updateLastCommand();
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);actions.setPadding(dp(4),dp(4),dp(4),dp(4));
        GradientDrawable dock=surface(0xD9272E37,40);dock.setStroke(dp(1),0x404F5C6D);actions.setBackground(dock);
        close=button("关锁",R.drawable.ic_lock);open=button("开锁",R.drawable.ic_unlock);
        actions.addView(close,new LinearLayout.LayoutParams(0,dp(60),1));
        View divider=new View(this);divider.setBackgroundColor(0x405F6A77);actions.addView(divider,new LinearLayout.LayoutParams(dp(1),dp(24)));
        actions.addView(open,new LinearLayout.LayoutParams(0,dp(60),1));
        LinearLayout.LayoutParams actionsParams=new LinearLayout.LayoutParams(-1,-2);actionsParams.setMargins(dp(28),dp(18),dp(28),dp(28));layout.addView(actions,actionsParams);
        LinearLayout sheet=new LinearLayout(this);sheet.setOrientation(LinearLayout.VERTICAL);sheet.setPadding(dp(18),0,dp(18),dp(16));
        GradientDrawable sheetBackground=surface(0xF51D232B,0);sheetBackground.setCornerRadii(new float[]{dp(28),dp(28),dp(28),dp(28),0,0,0,0});sheet.setBackground(sheetBackground);
        Button more=button("更多控制",R.drawable.ic_controls);more.setTextSize(14);more.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);more.setPadding(dp(8),0,dp(8),0);
        more.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_chevron_down,0);more.setStateDescription("已收起");more.setContentDescription("更多控制，点按展开");
        sheet.addView(more,new LinearLayout.LayoutParams(-1,dp(64)));
        HorizontalScrollView controlScroll=new HorizontalScrollView(this);controlScroll.setVisibility(View.GONE);controlScroll.setHorizontalScrollBarEnabled(true);controlScroll.setScrollbarFadingEnabled(false);controlScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);controlScroll.setFillViewport(false);controlScroll.setPadding(0,0,0,dp(8));
        LinearLayout controlRow=new LinearLayout(this);controlRow.setGravity(Gravity.CENTER_VERTICAL);controlScroll.addView(controlRow,new HorizontalScrollView.LayoutParams(-2,-2));
        addControl(controlRow,"开窗",R.drawable.ic_window_open,()->requestAction(()->key.windows(true)));
        addControl(controlRow,"关窗",R.drawable.ic_window_close,()->requestAction(()->key.windows(false)));
        addControl(controlRow,"后备箱",R.drawable.ic_trunk,()->requestAction(()->key.trunk()));
        addControl(controlRow,"洗车",R.drawable.ic_wash,()->openWash());
        Button nfc=addControl(controlRow,"NFC 贴片",R.drawable.ic_key_mono,()->startActivity(new Intent(this,NfcSetupActivity.class)));extraControls.remove(nfc);
        sheet.addView(controlScroll,new LinearLayout.LayoutParams(-1,-2));
        carSlot.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int drawerHeight=controlScroll.getVisibility()==View.VISIBLE?controlScroll.getMeasuredHeight():0;
            int otherHeight=layout.getMeasuredHeight()-(b-t)-drawerHeight;
            int viewportHeight=Math.max(dp(230),layout.getMinimumHeight()-otherHeight);
            vehicle3D.setSceneHeight(viewportHeight);
            scene.setVehicleLayout(layout.getTop()+t,viewportHeight);
            FrameLayout.LayoutParams sceneParams=(FrameLayout.LayoutParams)scene.getLayoutParams();
            int sceneHeight=Math.max(dp(520),layout.getTop()+t+viewportHeight+dp(48));
            if(sceneParams.height!=sceneHeight){sceneParams.height=sceneHeight;scene.setLayoutParams(sceneParams);}
        });
        more.setOnClickListener(v->{
            boolean expanded=controlScroll.getVisibility()!=View.VISIBLE;
            controlScroll.setVisibility(expanded?View.VISIBLE:View.GONE);
            more.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,expanded?R.drawable.ic_chevron_up:R.drawable.ic_chevron_down,0);
            more.setStateDescription(expanded?"已展开":"已收起");more.setContentDescription(expanded?"更多控制，点按收起":"更多控制，点按展开");
            if(expanded)sheet.post(()->sheet.requestRectangleOnScreen(
                new android.graphics.Rect(0,0,sheet.getWidth(),sheet.getHeight()),false));
            else{controlScroll.scrollTo(0,0);scroll.smoothScrollTo(0,0);}
        });
        layout.addView(sheet,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scroll);enable(false);
        getWindow().getInsetsController().setSystemBarsAppearance(0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);

        key=BleKey.get(this);
        open.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);requestAction(()->key.command(2));});
        close.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);requestAction(()->key.command(1));});
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},1);

    }
    private void updateStatus(String message){
        connectionMessage=message;
        String compact=message;
        if("已连接，可以操作".equals(message))compact="● 已连接";
        else if(message.startsWith("已连接，正在读取服务"))compact="正在读取服务…";
        else{
            int separator=message.indexOf('，');
            if(separator>0)compact=message.substring(0,separator);
        }
        status.setText(compact);
        status.setContentDescription(message+"，点按查看完整提示");
        status.setTooltipText(message);
    }
    private void requestAction(Runnable action){
        if(pendingAction!=null||key.operating())return;
        if(key.available()){action.run();return;}
        pendingAction=action;enable(false);if(washDialog!=null)enableWash(false);
        key.connect();
    }
    private void updateLastCommand(){
        if(lastCommand==null)return;
        int state=LocalLockStore.state(this);
        lastCommand.setText(state==LocalLockState.UNKNOWN?"尚无开关锁确认":
            "最近指令："+(state==LocalLockState.LOCKED?"关锁":"开锁"));
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
    private Button addControl(LinearLayout row,String title,int icon,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setTextSize(12);b.setTextColor(0xFFE0E6EF);
        b.setGravity(Gravity.CENTER);b.setPadding(dp(8),dp(12),dp(8),dp(10));
        android.graphics.drawable.Drawable symbol=getDrawable(icon).mutate();symbol.setBounds(0,0,dp(24),dp(24));
        b.setCompoundDrawables(null,symbol,null,null);b.setCompoundDrawablePadding(dp(10));
        b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x227E98B4),surface(0xFF282F38,14),null));b.setStateListAnimator(null);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setContentDescription(title);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(84),dp(86));
        if(row.getChildCount()>0)params.leftMargin=dp(8);
        row.addView(b,params);extraControls.add(b);b.setOnClickListener(v->action.run());return b;
    }
    private Button washButton(Context context,String text,int fill,int textColor){
        Button b=new Button(context);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(textColor);
        b.setGravity(Gravity.CENTER);b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setBackgroundTintList(null);b.setStateListAnimator(null);
        GradientDrawable background=surface(fill,14);
        if(fill==0xFF282F38)background.setStroke(dp(1),0x304F5C6D);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x307E98B4),background,null));
        return b;
    }
    private void addWashButton(LinearLayout panel,Button button,int margin){
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(margin);panel.addView(button,params);
    }
    private void openWash(){
        if(washDialog!=null)return;
        washLoaded=false;washFrom=Calendar.getInstance();washUntil=(Calendar)washFrom.clone();washUntil.add(Calendar.HOUR_OF_DAY,2);
        Context dialogContext=new ContextThemeWrapper(this,R.style.DuskDialogTheme);
        LinearLayout panel=new LinearLayout(dialogContext);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(16),dp(24),dp(24));
        LinearLayout heading=new LinearLayout(dialogContext);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("洗车模式",21,0xFFF2F5FA);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setAccessibilityHeading(true);
        heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button dismiss=washButton(dialogContext,"关闭",Color.TRANSPARENT,0xFFBBC5D0);dismiss.setPadding(dp(12),dp(12),dp(12),dp(12));heading.addView(dismiss,new LinearLayout.LayoutParams(-2,-2));panel.addView(heading);
        dismiss.setOnClickListener(v->{if(washDialog!=null)washDialog.dismiss();});
        LinearLayout warning=new LinearLayout(dialogContext);warning.setOrientation(LinearLayout.VERTICAL);warning.setPadding(dp(14),dp(12),dp(14),dp(12));
        GradientDrawable warningSurface=surface(0xFF302B26,14);warningSurface.setStroke(dp(1),0x505F4A36);warning.setBackground(warningSurface);
        TextView warningTitle=label("安全提醒",12,0xFFE5BA8E);warningTitle.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));warning.addView(warningTitle);
        TextView notice=label("洗车模式运行时车辆会解锁，离车不会自动锁车。请保管好车内物品。",13,0xFFE1CDB7);notice.setPadding(0,dp(6),0,0);notice.setLineSpacing(dp(2),1f);warning.addView(notice);
        LinearLayout.LayoutParams warningParams=new LinearLayout.LayoutParams(-1,-2);warningParams.topMargin=dp(12);panel.addView(warning,warningParams);
        LinearLayout stateRow=new LinearLayout(dialogContext);stateRow.setGravity(Gravity.CENTER_VERTICAL);
        washStatus=label("正在读取…",13,0xFFAAB6C4);washStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);washStatus.setPadding(0,dp(8),dp(8),dp(8));stateRow.addView(washStatus,new LinearLayout.LayoutParams(0,-2,1));
        washRefresh=washButton(dialogContext,"重新读取",Color.TRANSPARENT,0xFFBDD0E4);washRefresh.setTextSize(12);washRefresh.setPadding(dp(8),dp(12),0,dp(12));stateRow.addView(washRefresh,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams stateParams=new LinearLayout.LayoutParams(-1,-2);stateParams.topMargin=dp(8);panel.addView(stateRow,stateParams);
        washStart=washButton(dialogContext,"",0xFF282F38,0xFFE0E6EF);washEnd=washButton(dialogContext,"",0xFF282F38,0xFFE0E6EF);
        washStart.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);washEnd.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        addWashButton(panel,washStart,4);addWashButton(panel,washEnd,8);updateWashTimes();
        washStart.setOnClickListener(v->pickTime(true));washEnd.setOnClickListener(v->pickTime(false));
        washAutoExit=new Switch(dialogContext);washAutoExit.setText("蓝牙重连后自动退出");washAutoExit.setTextSize(14);washAutoExit.setTextColor(0xFFE0E6EF);washAutoExit.setMinHeight(dp(48));washAutoExit.setSwitchPadding(dp(12));
        int[][] switchStates=new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}};
        washAutoExit.setThumbTintList(new ColorStateList(switchStates,new int[]{0xFF616B77,0xFFBDD0E4,0xFF9AA6B4}));
        washAutoExit.setTrackTintList(new ColorStateList(switchStates,new int[]{0xFF343D48,0xFF526B85,0xFF46515E}));
        LinearLayout.LayoutParams switchParams=new LinearLayout.LayoutParams(-1,-2);switchParams.topMargin=dp(14);panel.addView(washAutoExit,switchParams);
        TextView hint=label("蓝牙断开后再次连接，将自动退出洗车模式。",12,0xFF9AA6B4);hint.setLineSpacing(dp(2),1f);panel.addView(hint);
        washSave=washButton(dialogContext,"保存洗车时段",0xFFBDD0E4,0xFF172331);addWashButton(panel,washSave,20);
        washStop=washButton(dialogContext,"停止洗车模式",0xFF282F38,0xFFE1B8AE);addWashButton(panel,washStop,8);
        ScrollView content=new ScrollView(dialogContext);content.setVerticalScrollBarEnabled(false);content.addView(panel);
        washDialog=new AlertDialog.Builder(dialogContext).create();washDialog.setView(content,0,0,0,0);
        washDialog.setOnDismissListener(d->{washDialog=null;washLoaded=false;pendingAction=null;enable(key.available());});washDialog.show();
        GradientDrawable dialogSurface=surface(0xFF1D232B,28);dialogSurface.setStroke(dp(1),0x304F5C6D);washDialog.getWindow().setBackgroundDrawable(dialogSurface);
        int availableWidth=getWindowManager().getCurrentWindowMetrics().getBounds().width();
        washDialog.getWindow().setLayout(Math.min(dp(440),availableWidth-dp(48)),-2);washDialog.getWindow().getDecorView().setClipToOutline(true);enableWash(key.available());
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
        washAutoExit.setAlpha(available&&washLoaded?1f:.48f);
        washRefresh.setEnabled(available);washRefresh.setAlpha(available?1f:.48f);
    }
    private void updateWashTimes(){
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd  HH:mm",Locale.CHINA);
        washStart.setText("开始时间\n"+format.format(washFrom.getTime()));washEnd.setText("结束时间\n"+format.format(washUntil.getTime()));
    }
    private void showWashPicker(AlertDialog picker){
        picker.show();picker.getWindow().setBackgroundDrawable(surface(0xFF1D232B,24));picker.getWindow().getDecorView().setClipToOutline(true);
        picker.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFBDD0E4);picker.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFBBC5D0);
    }
    private void pickTime(boolean start){
        Calendar current=start?washFrom:washUntil;
        DatePickerDialog date=new DatePickerDialog(this,R.style.DuskDialogTheme,(picker,year,month,day)->{
            TimePickerDialog time=new TimePickerDialog(this,R.style.DuskDialogTheme,(clock,hour,minute)->{
                current.set(year,month,day,hour,minute,0);current.set(Calendar.MILLISECOND,0);updateWashTimes();
            },current.get(Calendar.HOUR_OF_DAY),current.get(Calendar.MINUTE),true);
            showWashPicker(time);
        },current.get(Calendar.YEAR),current.get(Calendar.MONTH),current.get(Calendar.DAY_OF_MONTH));
        showWashPicker(date);
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
        if(results.length==1&&results[0]==0)key.connect();else updateStatus("需要附近设备权限，请重新打开应用授权");
    }
    @Override public void onStart(){super.onStart();sceneEffects.setAnimating(true);vehicle3D.setActive(true);updateLastCommand();key.attach(listener);if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED)key.connect();}
    @Override public void onStop(){vehicle3D.setActive(false);sceneEffects.setAnimating(false);pendingAction=null;key.detach(listener);super.onStop();}
    @Override public void onDestroy(){vehicle3D.destroy();super.onDestroy();}
}
