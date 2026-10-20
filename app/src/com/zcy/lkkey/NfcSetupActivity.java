package com.zcy.lkkey;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NfcSetupActivity extends Activity {
    private NfcAdapter adapter;
    private TextView status,localRecord;
    private Button write,cancel,notifications;
    private Switch enabled;
    private boolean updatingSwitch;
    private volatile boolean resumed;
    private final AtomicBoolean armed=new AtomicBoolean(),writing=new AtomicBoolean();

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(0xFF10151B);getWindow().setNavigationBarColor(0xFF10151B);
        getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFF10151B);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets safe=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            scroll.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;
        });
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(24),dp(24),dp(24),dp(32));scroll.addView(layout);
        Button back=button("返回钥匙");back.setOnClickListener(v->finish());layout.addView(back);
        layout.addView(text("NFC 贴片",26,0xFFF2F5FA,24));
        layout.addView(text("解锁手机，碰一下贴片\n按本地记录交替开关锁",18,0xFFBDD0E4,12));
        layout.addView(text("碰贴片后在后台执行，不打开钥匙首页。进度和结果通过无声音通知显示，请允许应用通知。",14,0xFFBBC5D0,12));
        localRecord=text("",16,0xFFBDD0E4,16);layout.addView(localRecord);
        layout.addView(text("根据本机最近确认的开关锁指令切换。首次默认开锁，确认后下次关锁；连接失败或没有确认时，下次仍执行原操作。",14,0xFFBBC5D0,12));
        layout.addView(text("本地记录可能与车辆实际状态不同：实体钥匙、其他手机和车辆自动锁车不会同步到这里。",14,0xFFBBC5D0,8));
        layout.addView(text("首次使用：点击下方写入按钮，再将贴片靠近手机背面的 NFC 感应区，保持贴近直到显示写入成功。",15,0xFFBBC5D0,20));
        layout.addView(text("写入会替换贴片现有内容，请使用空白或专用贴片。设置过程中不会发送开锁指令。",14,0xFFBBC5D0,12));
        enabled=new Switch(this);enabled.setText("碰贴片交替开关锁");enabled.setTextColor(0xFFF2F5FA);enabled.setTextSize(16);enabled.setPadding(0,dp(24),0,dp(12));layout.addView(enabled);
        enabled.setOnCheckedChangeListener((v,on)->{
            if(updatingSwitch)return;
            if(on&&!NfcAccess.prefs(this).getBoolean("written",false)){setSwitch(false);status.setText("请先写入一张控制贴片");return;}
            if(!NfcAccess.prefs(this).edit().putBoolean("enabled",on).commit()){setSwitch(NfcAccess.enabled(this));status.setText("无法保存设置，请重试");return;}
            status.setText(on?"已启用：离开此页面后碰贴片交替开关锁":"已停用：碰贴片不会操作车辆");
        });
        write=button("写入控制贴片");write.setOnClickListener(v->arm());layout.addView(write);
        cancel=button("取消写入");cancel.setVisibility(android.view.View.GONE);cancel.setOnClickListener(v->{armed.set(false);adapter.disableReaderMode(this);showIdle("已取消写入");});layout.addView(cancel);
        status=text("",15,0xFFBDD0E4,20);status.setGravity(Gravity.CENTER_HORIZONTAL);status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);layout.addView(status);
        Button settings=button("手机 NFC 设置");settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)));layout.addView(settings);
        notifications=button("操作结果通知设置");notifications.setOnClickListener(v->{
            if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},101);
            }else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        });layout.addView(notifications);
        layout.addView(text("日常使用：开启 NFC 和蓝牙，解锁手机后碰贴片。收到本次指令确认后，移开贴片再碰即可执行下一次操作，无需固定等待。执行期间的重复触碰会被忽略，不会排队；失败后移开再碰可以重试。首次授权“附近设备”或系统 NFC 启动提示时，请按提示允许。\n\n已有贴片无需重新写入。写入后先在车辆旁测试，确认可用再固定贴片；金属位置需使用抗金属贴片。",14,0xFFBBC5D0,20));
        setContentView(scroll);
        getWindow().getInsetsController().setSystemBarsAppearance(0,android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        adapter=NfcAdapter.getDefaultAdapter(this);
    }
    @Override public void onResume(){
        super.onResume();resumed=true;setSwitch(NfcAccess.enabled(this));localRecord.setText(LocalLockStore.summary(this));
        updateNotifications();
        if(!writing.get())showIdle(adapter==null?"这台手机不支持 NFC":!adapter.isEnabled()?"请先开启手机 NFC":NfcAccess.enabled(this)?"已启用：可写入贴片或返回钥匙":"点击写入按钮开始设置");
    }
    @Override public void onPause(){
        resumed=false;armed.set(false);if(adapter!=null)adapter.disableReaderMode(this);super.onPause();
    }
    private void updateNotifications(){notifications.setText(getSystemService(android.app.NotificationManager.class).areNotificationsEnabled()?"操作结果通知设置":"开启操作结果通知");}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==101)updateNotifications();}
    private void arm(){
        if(adapter==null){status.setText("这台手机不支持 NFC");return;}
        if(!adapter.isEnabled()){status.setText("请先在手机 NFC 设置中开启 NFC");return;}
        if(writing.get())return;
        try{NfcAccess.ensureToken(this);}catch(IllegalStateException error){status.setText(error.getMessage());return;}
        armed.set(true);write.setEnabled(false);enabled.setEnabled(false);cancel.setVisibility(android.view.View.VISIBLE);
        status.setText("请将贴片贴近手机背面，保持不动…");
        int flags=NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V;
        try{adapter.enableReaderMode(this,this::writeTag,flags,null);}
        catch(RuntimeException error){armed.set(false);showIdle("无法开始读取，请检查 NFC 设置后重试");}
    }
    private void writeTag(Tag tag){
        if(!resumed||!armed.compareAndSet(true,false)||!writing.compareAndSet(false,true))return;
        runOnUiThread(()->{cancel.setVisibility(android.view.View.GONE);status.setText("正在写入，请保持贴近…");});
        String result;
        try{
            NdefMessage message=NfcAccess.message(this);
            Ndef ndef=Ndef.get(tag);
            if(ndef!=null){
                try{
                    ndef.connect();
                    if(!resumed)throw new IOException("写入已取消，请重试");
                    if(!ndef.isWritable())throw new IOException("贴片已锁定或只读，请换一张可写贴片");
                    if(ndef.getMaxSize()<message.toByteArray().length)throw new IOException("贴片容量不足，请换一张贴片");
                    ndef.writeNdefMessage(message);
                    // Read back while the tag is still physically present.
                    NdefMessage actual=ndef.getNdefMessage();
                    if(actual==null||!java.util.Arrays.equals(actual.toByteArray(),message.toByteArray()))throw new IOException("写入校验失败，请保持贴近后重试");
                }finally{try{ndef.close();}catch(IOException ignored){}}
            }else{
                NdefFormatable format=NdefFormatable.get(tag);
                if(format==null)throw new IOException("不支持这张贴片，请使用可写 NDEF 标签");
                try{format.connect();if(!resumed)throw new IOException("写入已取消，请重试");format.format(message);}
                finally{try{format.close();}catch(IOException ignored){}}
            }
            if(!NfcAccess.prefs(this).edit().putBoolean("written",true).putBoolean("enabled",true).commit())throw new IOException("贴片已写入，但手机设置保存失败，请重试");
            result="写入成功，已启用贴片交替开关锁。\n请移开贴片，返回钥匙或桌面后再碰一次。";
        }catch(android.nfc.TagLostException lost){result="贴片移开太快，请点击写入后保持贴近重试";}
        catch(IOException error){result=error.getMessage()==null?"写入失败，请保持贴近后重试":error.getMessage();}
        catch(Exception error){result="写入失败，请确认贴片可写并重试";}
        writing.set(false);final String feedback=result;
        runOnUiThread(()->{
            if(isDestroyed()||isFinishing())return;
            if(resumed&&adapter!=null)adapter.disableReaderMode(this);
            setSwitch(NfcAccess.enabled(this));showIdle(feedback);
        });
    }
    private void showIdle(String message){write.setEnabled(adapter!=null&&adapter.isEnabled()&&!writing.get());enabled.setEnabled(!writing.get());cancel.setVisibility(android.view.View.GONE);status.setText(message);}
    private void setSwitch(boolean value){updatingSwitch=true;enabled.setChecked(value);updatingSwitch=false;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color,int top){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(color);text.setPadding(0,dp(top),0,dp(12));return text;}
    private Button button(String value){
        Button button=new Button(this);button.setText(value);button.setAllCaps(false);button.setTextSize(16);button.setTextColor(0xFFF2F5FA);button.setPadding(dp(16),dp(12),dp(16),dp(12));button.setMinHeight(dp(52));
        GradientDrawable background=new GradientDrawable();background.setColor(0xFF28323E);background.setCornerRadius(dp(14));button.setBackgroundTintList(null);button.setBackground(background);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(10);button.setLayoutParams(params);return button;
    }
}
