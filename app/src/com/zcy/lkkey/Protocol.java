package com.zcy.lkkey;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;

public final class Protocol {
    public static byte[] time(Calendar c) {
        return ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
            .putShort((short)c.get(Calendar.YEAR)).put((byte)(c.get(Calendar.MONTH)+1))
            .put((byte)c.get(Calendar.DAY_OF_MONTH)).put((byte)c.get(Calendar.HOUR_OF_DAY))
            .put((byte)c.get(Calendar.MINUTE)).put((byte)c.get(Calendar.SECOND)).array();
    }
    public static byte[] frame(int opcode, byte[] data, String advertisement, Calendar now) {
        try {
            byte[] time = time(now);
            byte[] sign = MessageDigest.getInstance("MD5").digest((hex(time)+advertisement).getBytes(StandardCharsets.UTF_8));
            int length = 25 + data.length;
            return ByteBuffer.allocate(9+length).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)opcode).putShort((short)(6+length)).putShort((short)length)
                .put((byte)0).put((byte)1).putShort((short)length)
                .put(time).put(sign).putShort((short)data.length).put(data).array();
        } catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public static String hex(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (byte b : data) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
}
