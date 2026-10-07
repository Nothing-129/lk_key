import com.zcy.lkkey.*;
import java.util.Calendar;
public class WashModeTest {
 public static void main(String[] args){
  Calendar start=Calendar.getInstance();start.clear();start.set(2026,9,4,10,30,0);
  Calendar end=(Calendar)start.clone();end.add(Calendar.HOUR_OF_DAY,2);
  byte[] active=WashMode.encode(start,end,true,start.getTimeInMillis()+1000);
  check(Protocol.hex(active).equals("ea070a040a1e00ea070a040c1e000101"));
  WashMode decoded=WashMode.decode(active);check(decoded.active&&decoded.autoExit&&decoded.start.getTimeInMillis()==start.getTimeInMillis()&&decoded.end.getTimeInMillis()==end.getTimeInMillis());
  check(WashMode.encode(start,end,false,start.getTimeInMillis()-1000)[14]==0);
  check(WashMode.encode(start,end,false,end.getTimeInMillis())[14]==0);
  byte[] stop=WashMode.encode(null,null,true,0);check(Protocol.hex(stop).equals("00000000000000000000000000000001"));
  check(WashMode.decode(stop).start==null&&!WashMode.decode(stop).active);
  try{WashMode.decode(new byte[15]);throw new AssertionError();}catch(IllegalArgumentException expected){}
  byte[] invalid=active.clone();invalid[2]=13;
  try{WashMode.decode(invalid);throw new AssertionError();}catch(IllegalArgumentException expected){}
  check(Protocol.hex(Protocol.frame(10,active,"00112233445566778899",start)).equals("0a2f00290000012900ea070a040a1e0028d522d351322d1caa4d67cab78760fa1000ea070a040a1e00ea070a040c1e000101"));
  check(Protocol.hex(Protocol.frame(3,new byte[0],"00112233445566778899",start)).equals("031f00190000011900ea070a040a1e0028d522d351322d1caa4d67cab78760fa0000"));
  check(Protocol.hex(Protocol.frame(2,new byte[]{1,(byte)0x88,0x13},"00112233445566778899",start)).equals("0222001c0000011c00ea070a040a1e0028d522d351322d1caa4d67cab78760fa0300018813"));
  System.out.println("PASS: windows, trunk, wash encoding/response/schedule/stop and golden frames");
 }
 static void check(boolean ok){if(!ok)throw new AssertionError();}
}
