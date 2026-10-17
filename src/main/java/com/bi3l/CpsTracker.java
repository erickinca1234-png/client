package com.bi3l;

import java.util.ArrayDeque;

/** Cuenta los clics reales por segundo. */
public final class CpsTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<Long>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<Long>();

    public static void addLeft() { LEFT.addLast(System.currentTimeMillis()); }
    public static void addRight() { RIGHT.addLast(System.currentTimeMillis()); }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.removeFirst();
        return q.size();
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }

    private CpsTracker() {}
}
