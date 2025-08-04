package com.holomap.map;

/**
 * Projects 2D grid coords (x,z) to an isometric 2.5D on-screen coordinate system.
 *
 * We keep it simple: a diamond (2:1) projection with configurable scale and angle.
 */
public final class IsometricProjector {
    private final float scale; // pixels per tile
    private final float elevation; // fake height scaling (optional)

    public IsometricProjector(float scale, float elevation) {
        this.scale = scale;
        this.elevation = elevation;
    }

    public float screenX(int gx, int gz) {
        // classic 2:1 iso projection
        return (gx - gz) * (scale * 0.5f);
    }

    public float screenY(int gx, int gz) {
        // classic 2:1 iso projection
        return (gx + gz) * (scale * 0.25f) - elevation;
    }
}