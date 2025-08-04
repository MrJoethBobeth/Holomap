package com.holomap.map;

import com.holomap.scan.BlockScanner;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

public final class MinimapRenderer {
    private static boolean enabled = true;

    private MinimapRenderer() {}

    public static void registerHudRender() {
        HudRenderCallback.EVENT.register(
                (drawContext, tickDelta) -> {
                    if (!enabled) return;

                    var client = MinecraftClient.getInstance();
                    if (client == null || client.player == null || client.world == null) return;

                    if (!MinimapData.get().isReady()) {
                        BlockScanner.scanAroundPlayerAsync(client.player, 48, 64);
                        return;
                    }

                    drawMinimap(drawContext);
                });
    }

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    private static void drawMinimap(DrawContext dc) {
        var client = MinecraftClient.getInstance();
        Window win = client.getWindow();
        int screenW = win.getScaledWidth();
        int screenH = win.getScaledHeight();

        int pad = 8;
        int viewSize = Math.min(180, Math.min(screenW, screenH) - 2 * pad);
        float centerX = screenW - pad - viewSize * 0.5f;
        float centerY = screenH - pad - viewSize * 0.5f;

        var data = MinimapData.get();
        int r = data.radius();
        int size = 2 * r + 1;
        int[] colors = data.colors();
        if (colors.length != size * size) return;

        // Draw backdrop using DrawContext
        int bg = ColorHelper.Argb.getArgb(96, 0, 0, 0);
        float half = viewSize * 0.5f;
        dc.fill(
                (int) (centerX - half),
                (int) (centerY - half),
                (int) (centerX + half),
                (int) (centerY + half),
                bg);

        // Setup rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        MatrixStack matrices = dc.getMatrices();
        matrices.push();

        // Use DrawContext's vertex consumers for custom geometry
        VertexConsumer vertexConsumer = dc.getVertexConsumers().getBuffer(RenderLayer.getGui());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float tileScale = Math.max(3.0f, (viewSize / (float) (r * 2 + 4)));
        float halfW = tileScale * 0.5f;   // diamond half width
        float halfH = tileScale * 0.25f;  // diamond half height

        // Draw center-first by rings
        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                int dzTop = ring - Math.abs(dx);
                drawCell(vertexConsumer, matrix, colors, r, dx, dzTop, centerX, centerY, size, halfW, halfH);
                if (dzTop != 0) {
                    drawCell(vertexConsumer, matrix, colors, r, dx, -dzTop, centerX, centerY, size, halfW, halfH);
                }
            }
        }

        matrices.pop();
        RenderSystem.disableBlend();
    }

    private static void drawCell(
            VertexConsumer vc,
            Matrix4f matrix,
            int[] colors,
            int radius,
            int gx,
            int gz,
            float centerX,
            float centerY,
            int size,
            float halfW,
            float halfH) {
        int ix = (gz + radius) * size + (gx + radius);
        if (ix < 0 || ix >= colors.length) return;
        int rgba = colors[ix];
        int a = (rgba >>> 24) & 0xFF;
        if (a == 0) return;

        int r = (rgba >>> 16) & 0xFF;
        int g = (rgba >>> 8) & 0xFF;
        int b = rgba & 0xFF;

        // 2:1 isometric placement
        float sx = centerX + (gx - gz) * (halfW);
        float sy = centerY + (gx + gz) * (halfH);

        // Diamond as a quad (top, right, bottom, left)
        vc.vertex(matrix, sx, sy - halfH, 0).color(r, g, b, a).next();
        vc.vertex(matrix, sx + halfW, sy, 0).color(r, g, b, a).next();
        vc.vertex(matrix, sx, sy + halfH, 0).color(r, g, b, a).next();
        vc.vertex(matrix, sx - halfW, sy, 0).color(r, g, b, a).next();
    }
}