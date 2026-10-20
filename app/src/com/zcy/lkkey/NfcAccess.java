package com.zcy.lkkey;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.security.SecureRandom;
import java.util.UUID;

final class NfcAccess {
    private static String ticket;
    private static long ticketAt,ticketOperation;
    private static final NfcTapGate TAP_GATE=new NfcTapGate();
    private static final Handler HANDLER=new Handler(Looper.getMainLooper());
    private static final String EXTRA_TICKET="com.zcy.lkkey.NFC_TICKET";
    static SharedPreferences prefs(Context context){return context.getSharedPreferences("nfc_tags",Context.MODE_PRIVATE);}
    static String token(Context context){return prefs(context).getString("token",null);}
    static String ensureToken(Context context){
        String token=token(context);if(token!=null)return token;
        byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);
        StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        token=out.toString();if(!prefs(context).edit().putString("token",token).commit())throw new IllegalStateException("无法保存贴片设置，请重试");return token;
    }
    static boolean enabled(Context context){return prefs(context).getBoolean("enabled",false);}
    static boolean matches(Context context,NdefMessage message){
        if(!enabled(context)||message==null||message.getRecords().length==0)return false;
        android.net.Uri uri=message.getRecords()[0].toUri();
        return uri!=null&&NfcTrigger.matches(uri.toString(),token(context));
    }
    static NdefMessage message(Context context){return new NdefMessage(new NdefRecord[]{
        NdefRecord.createUri(NfcTrigger.uri(ensureToken(context))),
        NdefRecord.createApplicationRecord(context.getPackageName())
    });}
    static boolean phoneUnlocked(Context context){
        return context.getSystemService(PowerManager.class).isInteractive()
            &&!context.getSystemService(KeyguardManager.class).isKeyguardLocked();
    }
    // Only successful physical reads create a short-lived, one-use in-process ticket.
    // The private background service accepts only this one-use handoff.
    static synchronized Intent controlIntent(Context context,Tag tag){
        if(tag==null)return null;
        long operation=TAP_GATE.begin();
        long presence=TAP_GATE.observed();
        try{
            NfcAdapter adapter=NfcAdapter.getDefaultAdapter(context);
            if(adapter==null){TAP_GATE.complete(operation);TAP_GATE.removed(presence);return null;}
            // This is removal debounce, not a cooldown between operations.
            // false means the physically-read tag has already left the field.
            boolean present=adapter.ignore(tag,250,()->TAP_GATE.removed(presence),HANDLER);
            if(!present)TAP_GATE.removed(presence);
        }catch(RuntimeException unavailable){
            TAP_GATE.complete(operation);TAP_GATE.removed(presence);return null;
        }
        if(operation==0)return null;
        return createTicket(context,operation);
    }
    private static synchronized Intent createTicket(Context context,long operation){
        ticketAt=SystemClock.elapsedRealtime();ticketOperation=operation;ticket=UUID.randomUUID().toString();
        String issued=ticket;
        HANDLER.postDelayed(()->expireTicket(issued,operation),5000);
        return new Intent(context,NfcService.class).putExtra(EXTRA_TICKET,ticket);
    }
    private static synchronized void expireTicket(String issued,long operation){
        if(!issued.equals(ticket))return;
        ticket=null;TAP_GATE.complete(operation);
    }
    static synchronized long consumeTicket(Intent intent){
        if(intent==null||ticket==null)return 0;
        String value;
        try{value=intent.getStringExtra(EXTRA_TICKET);}catch(RuntimeException invalid){return 0;}
        if(!ticket.equals(value))return 0;
        ticket=null;
        if(SystemClock.elapsedRealtime()-ticketAt>=5000||!TAP_GATE.pending(ticketOperation)){
            TAP_GATE.complete(ticketOperation);return 0;
        }
        return ticketOperation;
    }
    static void finishControl(long operation){TAP_GATE.complete(operation);}
}
