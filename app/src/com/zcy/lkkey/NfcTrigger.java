package com.zcy.lkkey;

import java.net.URI;
import java.net.URISyntaxException;

// A trigger identifier is local to this installation; it is not a vehicle key.
final class NfcTrigger {
    static String uri(String token){
        if(token==null||!token.matches("[0-9a-f]{32}"))throw new IllegalArgumentException("Invalid NFC identifier");
        // Keep the deployed tag URI; the app resolves the next command locally.
        return "lkkey://nfc/unlock?token="+token;
    }
    static boolean matches(String value,String token){
        if(value==null||token==null||!token.matches("[0-9a-f]{32}"))return false;
        try{
            URI uri=new URI(value);
            return "lkkey".equals(uri.getScheme())&&"nfc".equals(uri.getRawAuthority())
                &&"/unlock".equals(uri.getRawPath())&&("token="+token).equals(uri.getRawQuery())
                &&uri.getRawFragment()==null;
        }catch(URISyntaxException invalid){return false;}
    }
}
