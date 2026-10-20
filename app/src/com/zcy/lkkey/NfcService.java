package com.zcy.lkkey;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;

/** One authenticated NFC operation, with no activity launch or success toast. */
public class NfcService extends Service {
    static final String CHANNEL="nfc_control";
    static final int NOTIFICATION_ID=2002;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long operation;
    private int lastStartId;
    private boolean foreground;
    private NfcControl control;
    private Runnable timeout;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        lastStartId=startId;
        long incoming=NfcAccess.consumeTicket(intent);
        if(incoming==0){if(operation==0)stopSelfResult(lastStartId);return START_NOT_STICKY;}
        if(operation!=0){NfcAccess.finishControl(incoming);return START_NOT_STICKY;}
        operation=incoming;
        try{
            NotificationManager manager=getSystemService(NotificationManager.class);
            NotificationChannel channel=new NotificationChannel(CHANNEL,"NFC 贴片操作",NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null,null);channel.enableVibration(false);channel.setShowBadge(false);manager.createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID,notification("正在连接蓝牙…",true),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
            foreground=true;
            if(!NfcAccess.phoneUnlocked(this)||!NfcAccess.enabled(this)){finish(incoming,"请解锁手机并启用贴片控制后重试",false);return START_NOT_STICKY;}
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){finish(incoming,"请先打开 App 授予附近设备权限",false);return START_NOT_STICKY;}
            timeout=()->finish(incoming,"操作超时，请检查车辆状态后重新碰贴片",false);handler.postDelayed(timeout,25000);
            NfcControl started=NfcControl.start(this,incoming,new NfcControl.Observer(){
                public void changed(String message){if(operation==incoming)notifyStatus(message,true);}
                public void finished(String message,boolean confirmed){finish(incoming,message,confirmed);}
            });
            if(operation==incoming)control=started;
            else started.cancel();
        }catch(RuntimeException unavailable){finish(incoming,"后台操作未能启动，请检查应用权限后重试",false);}
        return START_NOT_STICKY;
    }
    private Notification notification(String message,boolean ongoing){
        PendingIntent app=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_key_mono)
            .setContentTitle("LYNK & CO · NFC").setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(app).setOngoing(ongoing).setAutoCancel(!ongoing).setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        if(!ongoing)builder.setTimeoutAfter(60000);
        return builder.build();
    }
    private void notifyStatus(String message,boolean ongoing){
        try{getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,notification(message,ongoing));}
        catch(RuntimeException ignored){/* Notification permission may be denied; control still terminates. */}
    }
    private void finish(long expected,String message,boolean confirmed){
        if(operation!=expected||operation==0)return;
        operation=0;
        if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}
        NfcControl previous=control;control=null;if(previous!=null)previous.cancel();
        NfcAccess.finishControl(expected);
        if(foreground){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;}
        notifyStatus(message,false);
        stopSelfResult(lastStartId);
    }
    @Override public void onDestroy(){
        if(timeout!=null)handler.removeCallbacks(timeout);
        long interrupted=operation;operation=0;
        NfcControl previous=control;control=null;if(previous!=null)previous.cancel();
        if(foreground)stopForeground(STOP_FOREGROUND_REMOVE);
        if(interrupted!=0){NfcAccess.finishControl(interrupted);notifyStatus("操作已中断，请检查车辆状态后重新碰贴片",false);}
        super.onDestroy();
    }
}
