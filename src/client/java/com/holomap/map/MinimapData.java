package com.holomap.map;

import net.minecraft.util.math.BlockPos;

public final class MinimapData {
    private static final MinimapData INSTANCE = new MinimapData();

    private BlockPos origin = BlockPos.ORIGIN;
    private int radius = 32;
    private int[] colors = new int[0];
    private int[] heights = new int[0];
    private boolean ready = false;

    private MinimapData() {}

    public static MinimapData get() {
        return INSTANCE;
    }

    public synchronized void setScanResult(BlockPos origin, int radius, int[] colors) {
        this.origin = origin.toImmutable();
        this.radius = radius;
        this.colors = colors;
        this.heights = new int[colors.length]; // Initialize heights to zero
        this.ready = true;
    }

    public synchronized void setScanResultWithHeight(BlockPos origin, int radius, int[] colors, int[] heights) {
        this.origin = origin.toImmutable();
        this.radius = radius;
        this.colors = colors;
        this.heights = heights;
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

    public synchronized int[] heights() {
        return heights;
    }

    public synchronized void reset() {
        ready = false;
        colors = new int[0];
        heights = new int[0];
    }
}