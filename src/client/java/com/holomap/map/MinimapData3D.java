package com.holomap.map;

import com.holomap.scan.BlockScanner3D;
import net.minecraft.util.math.BlockPos;

public final class MinimapData3D {
    private static final MinimapData3D INSTANCE = new MinimapData3D();

    private BlockPos origin = BlockPos.ORIGIN;
    private int horizontalRadius = 32;
    private int verticalRange = 64;
    private BlockScanner3D.Block3DData[][][] blocks = new BlockScanner3D.Block3DData[0][0][0];
    private boolean ready = false;
    private boolean meshDirty = true;

    private MinimapData3D() {}

    public static MinimapData3D get() {
        return INSTANCE;
    }

    public synchronized void setScanResult(BlockPos origin, int hr, int vr,
                                           BlockScanner3D.Block3DData[][][] blocks) {
        this.origin = origin.toImmutable();
        this.horizontalRadius = hr;
        this.verticalRange = vr;
        this.blocks = blocks;
        this.ready = true;
        this.meshDirty = true;
    }

    public synchronized boolean isReady() {
        return ready && blocks != null;
    }

    public synchronized BlockPos origin() { return origin; }
    public synchronized int horizontalRadius() { return horizontalRadius; }
    public synchronized int verticalRange() { return verticalRange; }
    public synchronized BlockScanner3D.Block3DData[][][] blocks() { return blocks; }

    public synchronized boolean isMeshDirty() { return meshDirty; }
    public synchronized void markMeshClean() { meshDirty = false; }

    public synchronized void reset() {
        ready = false;
        meshDirty = true;
        blocks = new BlockScanner3D.Block3DData[0][0][0];
    }
}