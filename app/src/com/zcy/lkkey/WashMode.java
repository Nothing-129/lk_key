package com.zcy.lkkey;

import java.util.Calendar;

public final class WashMode {
    public final Calendar start,end;
    public final boolean active,autoExit;
    private WashMode(Calendar start,Calendar end,boolean active,boolean autoExit){this.start=start;this.end=end;this.active=active;this.autoExit=autoExit;}
    public static byte[] encode(Calendar start,Calendar end,boolean autoExit,long now){
        byte[] data=new byte[16];
        if(start!=null&&end!=null){
            System.arraycopy(Protocol.time(start),0,data,0,7);System.arraycopy(Protocol.time(end),0,data,7,7);
            data[14]=(byte)(start.getTimeInMillis()<=now&&now<end.getTimeInMillis()?1:0);
        }
        data[15]=(byte)(autoExit?1:0);return data;
    }
    public static WashMode decode(byte[] data){
        if(data.length!=16)throw new IllegalArgumentException("洗车状态长度错误");
        return new WashMode(date(data,0),date(data,7),data[14]!=0,data[15]!=0);
    }
    private static Calendar date(byte[] data,int offset){
        int year=(data[offset]&255)|((data[offset+1]&255)<<8);if(year==0)return null;
        Calendar c=Calendar.getInstance();c.clear();c.setLenient(false);
        c.set(year,(data[offset+2]&255)-1,data[offset+3]&255,data[offset+4]&255,data[offset+5]&255,data[offset+6]&255);
        c.getTimeInMillis();return c;
    }
}
