package com.holomap.map;

import net.minecraft.util.math.BlockPos;

public final class MinimapData {
    private static final MinimapData INSTANCE = new MinimapData();

    private BlockPos origin = BlockPos.ORIGIN;
    private int radius = 32;
    private int[] colors = new int[0];
    private boolean ready = false;

    private MinimapData() {}

    public static MinimapData get() {
        return INSTANCE;
    }

    public synchronized void setScanResult(BlockPos origin, int radius, int[] colors) {
        this.origin = origin.toImmutable();
        this.radius = radius;
        this.colors = colors;
        this.ready = true;
    }

    public synchronized boolean isReady() {
        return ready && colors != null && colors.length > 0;
    }

    public synchronized BlockPos origin() {
        return origin;
    }

    public synchronized int radius() {
        return radius;
    }

    public synchronized int[] colors() {
        return colors;
    }

    public synchronized void reset() {
        ready = false;
        colors = new int[0];
    }
}