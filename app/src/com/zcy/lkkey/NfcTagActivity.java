package com.zcy.lkkey;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

public class NfcTagActivity extends Activity {
    private boolean resumed,reading,handled;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        overridePendingTransition(0,0);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
    @Override public void onResume(){super.onResume();resumed=true;readTag();}
    @Override public void onPause(){resumed=false;super.onPause();if(!isFinishing())finish();}
    @Override public void finish(){super.finish();overridePendingTransition(0,0);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(!reading){handled=false;readTag();}}
    private void readTag(){
        if(!resumed||reading||handled)return;
        handled=true;
        Intent intent=getIntent();
        if(!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())||!NfcAccess.phoneUnlocked(this)){reject("请解锁手机后重新碰贴片");return;}
        if(!NfcAccess.enabled(this)){reject("请在 App 的 NFC 贴片中完成写入并启用");return;}
        Tag tag;
        Ndef ndef;
        try{tag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);ndef=tag==null?null:Ndef.get(tag);}
        catch(RuntimeException invalid){reject("无法读取贴片，请重新碰贴片");return;}
        if(ndef==null){reject("未读取到真实贴片，请重新贴近手机");return;}
        reading=true;
        new Thread(()->{
            boolean valid=false;
            try{ndef.connect();valid=NfcAccess.matches(this,ndef.getNdefMessage());}
            catch(Exception ignored){/* Lost, stale and forged tags cannot unlock. */}
            finally{try{ndef.close();}catch(Exception ignored){}}
            final boolean accepted=valid;
            runOnUiThread(()->{
                reading=false;
                if(!resumed||isFinishing()||isDestroyed())return;
                if(!accepted){reject("贴片不匹配或已移开，请用本机写入的贴片重试");return;}
                if(!NfcAccess.phoneUnlocked(this)||!NfcAccess.enabled(this)){reject("请解锁手机并启用贴片控制后重试");return;}
                Intent control=NfcAccess.controlIntent(this,tag);
                if(control==null){reject("请等本次操作结束，移开贴片后再碰");return;}
                try{startForegroundService(control);finish();}
                catch(RuntimeException unavailable){
                    NfcAccess.finishControl(NfcAccess.consumeTicket(control));
                    reject("后台操作未能启动，请检查应用权限后重试");
                }
            });
        },"nfc-unlock-read").start();
    }
    private void reject(String message){android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show();finish();}
}
