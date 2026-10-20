package com.zcy.lkkey;

public class NfcTriggerTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String token="0123456789abcdef0123456789abcdef",valid=NfcTrigger.uri(token);
        check(NfcTrigger.matches(valid,token),"own tag must match");
        check(!NfcTrigger.matches(valid,"11111111111111111111111111111111"),"other installation rejected");
        String[] invalid={null,"","lkkey://nfc/unlock","https://nfc/unlock?token="+token,
            "lkkey://nfc:80/unlock?token="+token,"lkkey://user@nfc/unlock?token="+token,
            "lkkey://nfc/unlock/extra?token="+token,"lkkey://nfc/lock?token="+token,
            valid+"&action=lock",valid+"&token="+token,valid+"#fragment",
            "lkkey://nfc/unlock?token=%30"+token.substring(1),"lkkey://nfc/unlock?token=bad token"};
        for(String value:invalid)check(!NfcTrigger.matches(value,token),"unsafe URI accepted: "+value);
        check(!NfcTrigger.matches(valid,null),"unset installation rejected");
        try{NfcTrigger.uri("short");throw new AssertionError("invalid token accepted");}catch(IllegalArgumentException expected){}
        System.out.println("PASS: own tag accepted; other installation, malformed and altered commands rejected");
    }
}
