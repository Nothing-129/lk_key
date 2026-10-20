package com.zcy.lkkey;

import android.Manifest;
import android.bluetooth.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import java.util.*;

// One GATT session shared by the screen and the desktop widget.
final class BleKey {
    interface Listener {
        void changed(String message, boolean available);
        void result(int opcode);
        void failed(String message);
    }
    private static BleKey instance;
    static BleKey get(Context context) { if(instance==null)instance=new BleKey(context.getApplicationContext());return instance; }
    private final Context context;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners=new LinkedHashSet<>();
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writer;
    private boolean ready,busy,awaitingMtu;
    private int pending=-1;
    private Runnable commandCompletion;
    private boolean completionConfirmed;
    private String completionMessage="";
    boolean completionConfirmed(){return completionConfirmed;}
    String completionMessage(){return completionMessage;}
    private String actionLabel="";
    String acknowledgedAction(){return actionLabel;}
    private WashMode wash;
    WashMode wash(){return wash;}
    private String advertisement="", message="未连接";
    private BleKey(Context context){this.context=context;}
    private static UUID uuid(String id){return UUID.fromString("0000"+id+"-0000-1000-8000-00805F9B34FB");}
    boolean operating(){return pending!=-1&&pending!=6;}
    boolean available(){return ready&&!busy;}
    void attach(Listener listener){listeners.add(listener);listener.changed(message,available());}
    void detach(Listener listener){listeners.remove(listener);if(listeners.isEmpty())disconnect();}
    private String actionDisplayLabel(){return "关锁升窗".equals(actionLabel)?"关锁":actionLabel;}
    private void show(String value){message=value;Log.i("LkKey",value);for(Listener l:new ArrayList<>(listeners))l.changed(value,available());}
    private void fail(String value){disconnect(value);show(value);for(Listener l:new ArrayList<>(listeners))l.failed(value);}
    private void disconnect(){disconnect("操作已中断，请重新碰贴片");}
    private void disconnect(String reason){
        handler.removeCallbacksAndMessages(null);ready=false;busy=false;awaitingMtu=false;pending=-1;writer=null;advertisement="";wash=null;
        if(gatt!=null){BluetoothGatt old=gatt;gatt=null;try{old.disconnect();old.close();}catch(SecurityException denied){Log.w("LkKey","Connect permission revoked");}}
        completeCommand(false,reason);
    }
    void connect(){
        if(gatt!=null)return;
        if(context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){fail("请打开 App 授予附近设备权限");return;}
        BluetoothAdapter adapter=context.getSystemService(BluetoothManager.class).getAdapter();
        if(adapter==null||!adapter.isEnabled()){fail("请开启手机蓝牙");return;}
        BluetoothDevice target=null;
        for(BluetoothDevice d:adapter.getBondedDevices())if("LYNK_CO_03".equals(d.getName())){if(target!=null){fail("存在同名设备，请先确认目标");return;}target=d;}
        if(target==null){fail("未找到已配对的 LYNK_CO_03");return;}
        show("正在连接…");
        gatt=target.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE);
        handler.postDelayed(connectionTimeout,18000);
    }
    private final Runnable connectionTimeout=()->fail("连接超时，请靠近车辆后重试");
    private final Runnable commandTimeout=()->fail("未收到设备回复，请检查车辆状态");
    boolean command(int opcode){
        if(opcode!=1&&opcode!=2)return false;
        return perform(opcode,opcode==1?new byte[]{1,(byte)0x88,0x13}:new byte[0],opcode==1?"关锁升窗":"开锁");
    }
    boolean command(int opcode,Runnable completion){
        if((opcode!=1&&opcode!=2)||!available())return false;
        commandCompletion=completion;
        boolean sent=command(opcode);
        if(!sent)completeCommand(false,"指令未能发送，请重新碰贴片");
        return sent;
    }
    private void completeCommand(boolean confirmed,String message){
        Runnable completion=commandCompletion;commandCompletion=null;
        if(completion!=null){completionConfirmed=confirmed;completionMessage=message;completion.run();}
    }
    boolean windows(boolean open){return perform(open?2:1,new byte[]{1,(byte)0x88,0x13},open?"开窗":"关窗");}
    boolean trunk(){return perform(3,new byte[0],"后备箱");}
    boolean readWash(){return perform(10,new byte[0],"读取洗车模式");}
    boolean setWash(Calendar start,Calendar end,boolean autoExit){return perform(10,WashMode.encode(start,end,autoExit,System.currentTimeMillis()),start==null?"停止洗车模式":"设置洗车模式");}
    private boolean perform(int opcode,byte[] data,String label){
        if(!available())return false;actionLabel=label;send(opcode,data);return true;
    }
    private void send(int opcode,byte[] data){
        byte[] frame=Protocol.frame(opcode,data,advertisement,Calendar.getInstance());
        busy=true;pending=opcode;
        show(opcode==6?"正在同步设备时间…":"正在"+actionDisplayLabel()+"…");
        writer.setWriteType((writer.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)!=0?BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        writer.setValue(frame);Log.i("LkKey","TX opcode="+opcode+" bytes="+frame.length);
        if(!gatt.writeCharacteristic(writer)){fail("指令未能发送，请重试");return;}
        handler.postDelayed(commandTimeout,6000);
    }
    private final BluetoothGattCallback callback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int code,int state){handler.post(()->{
            if(g!=gatt)return;
            if(code==0&&state==BluetoothProfile.STATE_CONNECTED){show("已连接，正在读取服务…");if(!g.discoverServices())fail("读取服务失败，请重试");}
            else if(code!=0||state==BluetoothProfile.STATE_DISCONNECTED)fail("蓝牙连接已断开，请重试");
        });}
        @Override public void onServicesDiscovered(BluetoothGatt g,int code){handler.post(()->{
            if(g!=gatt)return;
            if(code!=0){fail("读取服务失败："+code);return;}
            BluetoothGattService service=g.getService(uuid("FFE0"));
            if(service==null||(writer=service.getCharacteristic(uuid("FFE2")))==null){fail("设备未提供蓝牙钥匙服务");return;}
            BluetoothGattCharacteristic notify=service.getCharacteristic(uuid("FFE1"));
            if(notify==null||!g.setCharacteristicNotification(notify,true)){fail("无法开启设备通知");return;}
            BluetoothGattDescriptor cccd=notify.getDescriptor(uuid("2902"));
            if(cccd==null){fail("设备缺少通知描述符");return;}
            cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if(!g.writeDescriptor(cccd))fail("订阅通知失败");
        });}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int code){handler.post(()->{
            if(g!=gatt)return;
            if(code!=0){fail("订阅通知失败："+code);return;}
            awaitingMtu=true;if(!g.requestMtu(64))fail("协商通信长度失败");
        });}
        @Override public void onMtuChanged(BluetoothGatt g,int mtu,int code){handler.post(()->{
            if(g!=gatt||!awaitingMtu)return;awaitingMtu=false;
            if(code!=0||mtu<53){fail("通信长度不足，不能发送指令");return;}
            Calendar now=Calendar.getInstance();byte[] data=Arrays.copyOf(Protocol.time(now),8);data[7]=(byte)(now.get(Calendar.DAY_OF_WEEK)-1);
            send(6,data);
        });}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int code){handler.post(()->{if(g==gatt&&code!=0)fail("指令写入失败："+code);});}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] value=c.getValue().clone();handler.post(()->{if(g==gatt)receive(value);});}
    };
    private void receive(byte[] value){
        if(value.length<4)return;
        int result=value[0]&255,opcode=value[1]&255,length=(value[2]&255)|((value[3]&255)<<8);
        Log.i("LkKey","RX opcode="+opcode+" result="+result+" length="+length);
        if(opcode!=pending||value.length<4+length)return;
        handler.removeCallbacks(commandTimeout);pending=-1;busy=false;
        if(result!=0){String[] errors={"成功","设备执行失败","签名超时","签名错误","密码错误","无设备操作权限","绑定密码已变更","未知命令"};fail(result<errors.length?errors[result]:"设备错误："+result);return;}
        if(opcode==6){
            if(length<17){fail("设备回复缺少授权状态");return;}
            advertisement=Protocol.hex(Arrays.copyOfRange(value,10,20));
            if(value[20]!=1){fail("请先在原小程序完成设备授权");return;}
            ready=true;handler.removeCallbacks(connectionTimeout);show("已连接，可以操作");
        }else{
            if(opcode==10){
                try{wash=WashMode.decode(Arrays.copyOfRange(value,4,4+length));}
                catch(IllegalArgumentException invalid){fail("设备返回的洗车状态无效，请重新读取");return;}
            }
            boolean recorded=LocalLockStore.acknowledge(context,result,opcode,actionLabel);
            String confirmation=(opcode==10?"洗车模式状态已更新":"设备已确认"+actionDisplayLabel()+"指令")+(recorded?"":"，本地记录保存失败");
            completeCommand(true,confirmation);
            show(confirmation);
            for(Listener l:new ArrayList<>(listeners))l.result(opcode);
        }
    }
}
