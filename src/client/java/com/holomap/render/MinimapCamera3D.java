package com.holomap.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MinimapCamera3D {
    private static final MinimapCamera3D INSTANCE = new MinimapCamera3D();

    private float lastPlayerYaw = 0f;
    private float currentYaw = 0f;
    private final float distance = 24f;      // Closer camera
    private final float height = 16f;        // Lower camera
    private final float pitch = -45f;        // Steeper angle

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private boolean matricesDirty = true;

    private MinimapCamera3D() {}

    public static MinimapCamera3D get() {
        return INSTANCE;
    }

    public void update() {
        var player = MinecraftClient.getInstance().player;
        if (player == null) return;

        float playerYaw = player.getYaw();

        // Smooth rotation with better responsiveness
        float yawDiff = MathHelper.wrapDegrees(playerYaw - currentYaw);
        if (Math.abs(yawDiff) > 1f) {
            currentYaw += yawDiff * 0.15f; // Slightly faster smoothing
            currentYaw = MathHelper.wrapDegrees(currentYaw);
            matricesDirty = true;
        }

        lastPlayerYaw = playerYaw;
    }

    public Matrix4f getViewProjectionMatrix(int viewportWidth, int viewportHeight) {
        if (matricesDirty) {
            updateMatrices(viewportWidth, viewportHeight);
            matricesDirty = false;
        }
        return new Matrix4f(viewProjectionMatrix); // Return copy to avoid modifications
    }

    private void updateMatrices(int viewportWidth, int viewportHeight) {
        // Camera position orbiting around center
        float yawRad = (float) Math.toRadians(currentYaw);
        float pitchRad = (float) Math.toRadians(pitch);

        Vector3f cameraPos = new Vector3f(
                (float) (Math.sin(yawRad) * distance * Math.cos(pitchRad)),
                height,
                (float) (Math.cos(yawRad) * distance * Math.cos(pitchRad))
        );

        Vector3f target = new Vector3f(0, 4, 0); // Look slightly above ground level
        Vector3f up = new Vector3f(0, 1, 0);

        // View matrix
        viewMatrix.identity().lookAt(cameraPos, target, up);

        // Orthographic projection for minimap (can switch to perspective if preferred)
        float size = 20f; // Orthographic size
        projectionMatrix.identity().ortho(-size, size, -size, size, 1f, 100f);

        // Combined matrix
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
    }

    public void forceUpdate() {
        matricesDirty = true;
    }
}