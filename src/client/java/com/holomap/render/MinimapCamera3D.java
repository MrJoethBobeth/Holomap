package com.holomap.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class MinimapCamera3D {
    private static final MinimapCamera3D INSTANCE = new MinimapCamera3D();

    private float currentYaw = 0f;
    private final float distance = 25f;
    private final float height = 18f;
    private final float pitch = -40f;

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
        float yawDiff = MathHelper.wrapDegrees(playerYaw - currentYaw);
        if (Math.abs(yawDiff) > 1f) {
            currentYaw += yawDiff * 0.15f;
            currentYaw = MathHelper.wrapDegrees(currentYaw);
            matricesDirty = true;
            MinimapRenderer3D.markDirty(); // Mark for redraw when camera changes
        }
    }

    public Matrix4f getViewProjectionMatrix(int viewportSize) {
        if (matricesDirty) {
            updateMatrices(viewportSize);
            matricesDirty = false;
        }
        return new Matrix4f(viewProjectionMatrix);
    }

    // Project 3D world coordinates to 2D screen coordinates
    public Vector3f projectToScreen(float worldX, float worldY, float worldZ, int viewportSize) {
        Vector4f worldPos = new Vector4f(worldX, worldY, worldZ, 1.0f);
        Vector4f projected = new Vector4f();

        getViewProjectionMatrix(viewportSize).transform(worldPos, projected);

        // Perspective divide
        if (projected.w != 0) {
            projected.x /= projected.w;
            projected.y /= projected.w;
            projected.z /= projected.w;
        }

        // Convert from NDC (-1 to 1) to screen coordinates (0 to viewportSize)
        float screenX = (projected.x + 1.0f) * 0.5f * viewportSize;
        float screenY = (1.0f - projected.y) * 0.5f * viewportSize; // Flip Y

        return new Vector3f(screenX, screenY, projected.z);
    }

    private void updateMatrices(int viewportSize) {
        // Camera position
        float yawRad = (float) Math.toRadians(currentYaw);
        float pitchRad = (float) Math.toRadians(pitch);

        Vector3f cameraPos = new Vector3f(
                (float) (Math.sin(yawRad) * distance * Math.cos(pitchRad)),
                height,
                (float) (Math.cos(yawRad) * distance * Math.cos(pitchRad))
        );

        Vector3f target = new Vector3f(0, 2, 0);
        Vector3f up = new Vector3f(0, 1, 0);

        // View matrix
        viewMatrix.identity().lookAt(cameraPos, target, up);

        // Orthographic projection for isometric view
        float size = 18f; // Adjust this to change zoom level
        projectionMatrix.identity().ortho(-size, size, -size, size, 1f, 100f);

        // Combined matrix
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
    }

    public void forceUpdate() {
        matricesDirty = true;
    }
}