package com.zcy.lkkey;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.*;

public class WidgetService extends Service {
    static final String OPEN="com.zcy.lkkey.OPEN", CLOSE="com.zcy.lkkey.CLOSE";
    private BleKey key;
    private boolean sent,finished;
    private int opcode;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final BleKey.Listener listener=new BleKey.Listener(){
        public void changed(String message,boolean available){
            if(finished)return;
            KeyWidget.update(WidgetService.this,message,true);
            if(available&&!sent){sent=true;key.command(opcode);}
        }
        public void result(int command){if(sent&&command==opcode)finish(command==2?"开锁指令已确认":"关锁升窗指令已确认");}
        public void failed(String message){finish(message);}
    };
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||(!OPEN.equals(intent.getAction())&&!CLOSE.equals(intent.getAction()))){stopSelf();return START_NOT_STICKY;}
        if(opcode!=0)return START_NOT_STICKY;
        opcode=OPEN.equals(intent.getAction())?2:1;
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            finish("请打开 App 授予附近设备权限");return START_NOT_STICKY;
        }
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("widget_control","桌面钥匙操作",NotificationManager.IMPORTANCE_LOW));
        PendingIntent app=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,"widget_control").setSmallIcon(R.drawable.ic_key_mono)
            .setContentTitle("LYNK & CO").setContentText(opcode==2?"正在执行开锁":"正在执行关锁升窗")
            .setContentIntent(app).setOngoing(true).build();
        startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        handler.postDelayed(()->finish("操作超时，请检查车辆状态"),25000);
        key=BleKey.get(this);
        if(key.operating()){finish("已有指令执行中，请稍后再试");return START_NOT_STICKY;}
        key.attach(listener);
        if(!finished)key.connect();
        return START_NOT_STICKY;
    }
    private void finish(String message){
        if(finished)return;finished=true;
        handler.removeCallbacksAndMessages(null);
        KeyWidget.update(this,message,false);
        if(key!=null)key.detach(listener);
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        if(key!=null)key.detach(listener);
        if(!finished)KeyWidget.update(this,"操作已中断，请检查车辆状态",false);
        super.onDestroy();
    }
}
