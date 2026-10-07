import com.zcy.lkkey.Protocol;
import java.util.*;
public class ProtocolTest {
 public static void main(String[] args) {
  Calendar c=Calendar.getInstance();c.set(2026,9,4,10,30,0);
  String tail="1f00190000011900ea070a040a1e0028d522d351322d1caa4d67cab78760fa0000";
  check(Protocol.hex(Protocol.frame(1,new byte[0],"00112233445566778899",c)).equals("01"+tail));
  check(Protocol.hex(Protocol.frame(2,new byte[0],"00112233445566778899",c)).equals("02"+tail));
  check(Protocol.hex(Protocol.frame(1,new byte[]{1,(byte)0x88,0x13},"00112233445566778899",c)).equals("0122001c0000011c00ea070a040a1e0028d522d351322d1caa4d67cab78760fa0300018813"));
  System.out.println("PASS: open/close and 5-second window-close golden frames");
 }
 static void check(boolean ok){if(!ok)throw new AssertionError();}
}
