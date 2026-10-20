package com.zcy.lkkey;

import android.content.Context;
import android.content.SharedPreferences;

final class LocalLockStore {
    private static SharedPreferences prefs(Context context){return context.getSharedPreferences("local_lock_state",Context.MODE_PRIVATE);}
    static int state(Context context){return prefs(context).getInt("last_confirmed",LocalLockState.UNKNOWN);}
    static int nextCommand(Context context){return LocalLockState.nextCommand(state(context));}
    static boolean acknowledge(Context context,int result,int opcode,String action){
        int before=state(context),after=LocalLockState.afterAcknowledgement(before,result,opcode,action);
        if(after==before)return true;
        return prefs(context).edit().putInt("last_confirmed",after).commit();
    }
    static String summary(Context context){
        int state=state(context);
        if(state==LocalLockState.UNLOCKED)return "本地记录：上次确认开锁\n下次碰贴片：关锁";
        if(state==LocalLockState.LOCKED)return "本地记录：上次确认关锁\n下次碰贴片：开锁";
        return "本地记录：尚无开关锁确认\n首次碰贴片：开锁";
    }
    static String nextLabel(Context context){return nextCommand(context)==1?"关锁":"开锁";}
}
