package com.holomap.render;

import com.holomap.map.MinimapData3D;
import com.holomap.scan.BlockScanner3D;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

public final class MinimapRenderer3D {
    private static boolean enabled = true;
    private static BlockMeshBuilder3D.BlockMesh cachedMesh = null;

    private MinimapRenderer3D() {}

    public static void registerHudRender() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!enabled) return;

            var client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.world == null) return;

            if (!MinimapData3D.get().isReady()) {
                BlockScanner3D.scanAroundPlayerAsync(client.player, 24, 32); // Reduced for performance
                return;
            }

            MinimapCamera3D.get().update();
            drawMinimap3D(drawContext);
        });
    }

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    private static void drawMinimap3D(DrawContext dc) {
        var client = MinecraftClient.getInstance();
        Window win = client.getWindow();
        int screenW = win.getScaledWidth();
        int screenH = win.getScaledHeight();

        // Minimap area
        int pad = 8;
        int viewSize = Math.min(200, Math.min(screenW, screenH) - 2 * pad);
        int viewX = screenW - pad - viewSize;
        int viewY = screenH - pad - viewSize;

        // Backdrop
        int bg = ColorHelper.Argb.getArgb(128, 0, 0, 0);
        dc.fill(viewX, viewY, viewX + viewSize, viewY + viewSize, bg);

        // Build or update mesh
        var data = MinimapData3D.get();
        if (cachedMesh == null || data.isMeshDirty()) {
            cachedMesh = BlockMeshBuilder3D.buildMesh();
            data.markMeshClean();
        }

        if (cachedMesh != null && !cachedMesh.vertices.isEmpty()) {
            renderMesh3D(dc, cachedMesh, viewX, viewY, viewSize);
        }
    }

    private static void renderMesh3D(DrawContext dc, BlockMeshBuilder3D.BlockMesh mesh,
                                     int viewX, int viewY, int viewSize) {
        MatrixStack matrices = dc.getMatrices();
        matrices.push();

        // Translate to minimap area center
        matrices.translate(viewX + viewSize / 2f, viewY + viewSize / 2f, 0);

        // Apply 3D camera transformation
        Matrix4f mvpMatrix = MinimapCamera3D.get().getViewProjectionMatrix(viewSize, viewSize);
        matrices.multiplyPositionMatrix(mvpMatrix);

        // Setup 3D rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LESS

        // Bind block atlas texture
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

        // Create buffer and render triangles
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES,
                VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Add vertices without manual transformation - let the vertex shader handle it
        for (int i = 0; i < mesh.indices.size(); i += 3) {
            for (int j = 0; j < 3; j++) {
                var vertex = mesh.vertices.get(mesh.indices.get(i + j));

                // Scale down for minimap (blocks are too big otherwise)
                float scale = viewSize / 120f; // Adjust this to change minimap zoom
                float x = vertex.x * scale;
                float y = vertex.y * scale;
                float z = vertex.z * scale;

                buf.vertex(matrix, x, y, z)
                        .texture(vertex.u, vertex.v)
                        .color(vertex.r, vertex.g, vertex.b, vertex.a);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }
}