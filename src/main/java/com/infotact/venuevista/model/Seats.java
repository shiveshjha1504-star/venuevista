package com.infotact.venuevista.model;

public final class Seats {
    private Seats() {}

    /** 1 -> A, 26 -> Z, 27 -> AA ... */
    public static String rowLabel(int n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            int m = (n - 1) % 26;
            sb.insert(0, (char) ('A' + m));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }
}
