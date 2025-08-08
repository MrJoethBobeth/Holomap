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
    private final float pitch = -40f; // Keep the isometric angle

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
            MinimapRenderer3D.markDirty();
        }
    }

    public Matrix4f getViewProjectionMatrix(int viewportSize) {
        if (matricesDirty) {
            updateMatrices(viewportSize);
            matricesDirty = false;
        }
        return new Matrix4f(viewProjectionMatrix);
    }

    public Vector3f projectToScreen(float worldX, float worldY, float worldZ, int viewportSize) {
        Vector4f worldPos = new Vector4f(worldX, worldY, worldZ, 1.0f);
        Vector4f projected = new Vector4f();

        getViewProjectionMatrix(viewportSize).transform(worldPos, projected);

        // Perspective divide
        if (Math.abs(projected.w) > 1e-6f) {
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
        // Restore the original isometric camera positioning logic but with consistent coordinates
        // Fix camera orientation - Minecraft yaw is different from standard math
        // Minecraft: 0=South, 90=West, 180=North, 270=East
        // We need to adjust this for proper minimap orientation
        float adjustedYaw = currentYaw + 180f; // Flip the direction like original
        float yawRad = (float) Math.toRadians(adjustedYaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Camera position - using original isometric positioning logic
        Vector3f cameraPos = new Vector3f(
                (float) (-Math.sin(yawRad) * distance * Math.cos(pitchRad)), // Keep original X calculation
                height, // Keep height constant for isometric view
                (float) (Math.cos(yawRad) * distance * Math.cos(pitchRad))  // Keep original Z calculation
        );

        Vector3f target = new Vector3f(0, 2, 0);
        Vector3f up = new Vector3f(0, 1, 0);

        // View matrix - restore original lookAt
        viewMatrix.identity().lookAt(cameraPos, target, up);

        // Orthographic projection for isometric view - restore original values
        float size = 18f; // Keep original size
        projectionMatrix.identity().ortho(-size, size, -size, size, 1f, 100f);

        // Combined matrix
        viewProjectionMatrix.set(projectionMatrix).mul(viewMatrix);
    }

    public void forceUpdate() {
        matricesDirty = true;
    }
}