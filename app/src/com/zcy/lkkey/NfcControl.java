package com.zcy.lkkey;

import android.content.Context;

// Own the BLE listener until this NFC operation completes, even if another NFC
// entry briefly covers MainActivity. Repeated taps never replace or queue it.
final class NfcControl {
    interface Observer {void changed(String message);void finished(String message,boolean confirmed);}
    private final Context context;
    private final long operation;
    private final BleKey key;
    private final Observer observer;
    private boolean sent,finished;
    private final BleKey.Listener listener=new BleKey.Listener(){
        public void changed(String message,boolean available){
            if(finished)return;
            observer.changed(message);
            if(sent||!available)return;
            sent=true;int opcode=LocalLockStore.nextCommand(context);
            if(!key.command(opcode,()->finish(key.completionMessage(),key.completionConfirmed())))finish("指令未能发送，请重新碰贴片",false);
        }
        public void result(int opcode){/* Completion belongs to the specific command. */}
        public void failed(String message){finish(message,false);}
    };
    private NfcControl(Context context,long operation,Observer observer){this.context=context.getApplicationContext();this.operation=operation;this.observer=observer;key=BleKey.get(context);}
    static NfcControl start(Context context,long operation,Observer observer){
        NfcControl control=new NfcControl(context,operation,observer);
        if(control.key.operating()){control.finish("已有指令执行中，请确认后重新碰贴片",false);return control;}
        try{
            control.key.attach(control.listener);
            if(!control.finished)control.key.connect();
        }catch(RuntimeException unavailable){control.finish("无法连接蓝牙，请检查附近设备权限后重试",false);}
        return control;
    }
    void cancel(){finish("操作已中断，请重新碰贴片",false);}
    private void finish(String message,boolean confirmed){
        if(finished)return;finished=true;
        NfcAccess.finishControl(operation);key.detach(listener);
        observer.finished(message,confirmed);
    }
}
