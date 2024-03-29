package com.tencent.msdk.dns.report;

import java.util.Random;

public class Session {
    private static String sessionId;

    public static void setSessionId() {
        final String sampleAlphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final Random random = new Random();
        char[] buf = new char[12];
        for (int i = 0; i < 12; i++) {
            buf[i] = sampleAlphabet.charAt(random.nextInt(sampleAlphabet.length()));
        }
        sessionId = new String(buf);
    }

    public static String getSessionId() {
        return sessionId;
    }
}
