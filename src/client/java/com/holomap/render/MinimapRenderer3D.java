package com.holomap.render;

import com.holomap.map.MinimapData3D;
import com.holomap.scan.BlockScanner3D;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MinimapRenderer3D {
    private static boolean enabled = true;
    private static NativeImageBackedTexture cachedTexture = null;
    private static Identifier textureId = null;
    private static boolean needsRedraw = true;

    private MinimapRenderer3D() {}

    public static void registerHudRender() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!enabled) return;

            var client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.world == null) return;

            if (!MinimapData3D.get().isReady()) {
                BlockScanner3D.scanAroundPlayerAsync(client.player, 24, 32);
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
        var window = client.getWindow();
        int screenW = window.getScaledWidth();
        int screenH = window.getScaledHeight();

        // Minimap area
        int pad = 8;
        int viewSize = Math.min(200, Math.min(screenW, screenH) - 2 * pad);
        int viewX = screenW - pad - viewSize;
        int viewY = screenH - pad - viewSize;

        // Background
        int bg = ColorHelper.Argb.getArgb(180, 0, 0, 0);
        dc.fill(viewX, viewY, viewX + viewSize, viewY + viewSize, bg);

        // Update texture if needed
        var data = MinimapData3D.get();
        if (needsRedraw || cachedTexture == null || data.isMeshDirty()) {
            updateTexture(viewSize);
            data.markMeshClean();
            needsRedraw = false;
        }

        // Render the cached texture
        if (cachedTexture != null && textureId != null) {
            // Crop to viewport bounds
            int cropX = Math.max(0, viewX);
            int cropY = Math.max(0, viewY);
            int cropW = Math.min(viewSize, screenW - cropX);
            int cropH = Math.min(viewSize, screenH - cropY);

            if (cropW > 0 && cropH > 0) {
                dc.drawTexture(textureId, cropX, cropY, 0, 0, cropW, cropH, viewSize, viewSize);
            }
        }
    }

    private static void updateTexture(int size) {
        // Clean up old texture
        if (cachedTexture != null) {
            cachedTexture.close();
        }

        // Create new texture
        NativeImage image = new NativeImage(size, size, false);

        // Clear to transparent
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setColor(x, y, 0x00000000);
            }
        }

        // Render to image
        renderToImage(image, size);

        // Upload to GPU
        cachedTexture = new NativeImageBackedTexture(image);
        if (textureId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(textureId);
        }
        textureId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("holomap_minimap", cachedTexture);
    }

    private static void renderToImage(NativeImage image, int size) {
        var data = MinimapData3D.get();
        if (!data.isReady()) return;

        List<RenderedFace> faces = buildFaceList(size);

        // Sort by depth (back to front)
        faces.sort(Comparator.comparingDouble((RenderedFace f) -> f.avgDepth).reversed());

        // Render each face to the image
        for (RenderedFace face : faces) {
            drawPolygonToImage(image, face.xPoints, face.yPoints, face.color, size);
        }
    }

    private static List<RenderedFace> buildFaceList(int viewSize) {
        var data = MinimapData3D.get();
        if (!data.isReady()) return new ArrayList<>();

        List<RenderedFace> faces = new ArrayList<>();
        var blocks = data.blocks();
        int hr = data.horizontalRadius();
        int vr = data.verticalRange();

        var camera = MinimapCamera3D.get();

        for (int x = 0; x < blocks.length; x++) {
            for (int y = 0; y < blocks[x].length; y++) {
                for (int z = 0; z < blocks[x][y].length; z++) {
                    var blockData = blocks[x][y][z];
                    if (blockData == null || blockData.state.isAir()) continue;

                    // World position (centered on origin)
                    float worldX = x - hr;
                    float worldY = y - vr/2f;
                    float worldZ = z - hr;

                    addBlockFaces(faces, blockData, worldX, worldY, worldZ, camera, viewSize);
                }
            }
        }

        return faces;
    }

    private static void addBlockFaces(List<RenderedFace> faces, BlockScanner3D.Block3DData blockData,
                                      float x, float y, float z, MinimapCamera3D camera, int viewSize) {
        boolean[] visibleFaces = blockData.visibleFaces;

        // Define face vertices for a unit cube
        float[][][] faceVertices = {
                // Down face (Y-)
                {{x, y, z+1}, {x+1, y, z+1}, {x+1, y, z}, {x, y, z}},
                // Up face (Y+)
                {{x, y+1, z}, {x+1, y+1, z}, {x+1, y+1, z+1}, {x, y+1, z+1}},
                // North face (Z-)
                {{x+1, y, z}, {x, y, z}, {x, y+1, z}, {x+1, y+1, z}},
                // South face (Z+)
                {{x, y, z+1}, {x+1, y, z+1}, {x+1, y+1, z+1}, {x, y+1, z+1}},
                // West face (X-)
                {{x, y, z}, {x, y, z+1}, {x, y+1, z+1}, {x, y+1, z}},
                // East face (X+)
                {{x+1, y, z+1}, {x+1, y, z}, {x+1, y+1, z}, {x+1, y+1, z+1}}
        };

        // Get block color based on block type
        int blockColor = getBlockColor(blockData.state.getBlock().getName().getString());

        int[] faceColors = {
                adjustBrightness(blockColor, 0.5f), // Down - darker
                adjustBrightness(blockColor, 1.0f), // Up - full brightness
                adjustBrightness(blockColor, 0.8f), // North - medium
                adjustBrightness(blockColor, 0.8f), // South - medium
                adjustBrightness(blockColor, 0.6f), // West - darker
                adjustBrightness(blockColor, 0.6f)  // East - darker
        };

        for (int i = 0; i < 6; i++) {
            if (!visibleFaces[i]) continue;

            // Project face vertices to screen
            Vector3f[] screenPoints = new Vector3f[4];
            float totalDepth = 0;
            boolean allInFront = true;

            for (int j = 0; j < 4; j++) {
                float[] vertex = faceVertices[i][j];
                screenPoints[j] = camera.projectToScreen(vertex[0], vertex[1], vertex[2], viewSize);
                totalDepth += screenPoints[j].z;

                // Check if behind camera
                if (screenPoints[j].z > 1.0f) {
                    allInFront = false;
                    break;
                }
            }

            if (!allInFront) continue;

            // Convert to integer arrays for polygon drawing
            int[] xPoints = new int[4];
            int[] yPoints = new int[4];

            for (int j = 0; j < 4; j++) {
                xPoints[j] = Math.round(screenPoints[j].x);
                yPoints[j] = Math.round(screenPoints[j].y);
            }

            float avgDepth = totalDepth / 4.0f;
            faces.add(new RenderedFace(xPoints, yPoints, avgDepth, faceColors[i]));
        }
    }

    private static void drawPolygonToImage(NativeImage image, int[] xPoints, int[] yPoints, int color, int size) {
        if (xPoints.length != 4) return;

        // Find bounding box
        int minX = Math.max(0, Math.min(Math.min(Math.min(xPoints[0], xPoints[1]), xPoints[2]), xPoints[3]));
        int maxX = Math.min(size - 1, Math.max(Math.max(Math.max(xPoints[0], xPoints[1]), xPoints[2]), xPoints[3]));
        int minY = Math.max(0, Math.min(Math.min(Math.min(yPoints[0], yPoints[1]), yPoints[2]), yPoints[3]));
        int maxY = Math.min(size - 1, Math.max(Math.max(Math.max(yPoints[0], yPoints[1]), yPoints[2]), yPoints[3]));

        // Scanline fill
        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();

            // Find intersections with polygon edges
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                int y1 = yPoints[i], y2 = yPoints[j];
                int x1 = xPoints[i], x2 = xPoints[j];

                if ((y1 <= y && y < y2) || (y2 <= y && y < y1)) {
                    if (y1 != y2) {
                        int x = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                        if (x >= 0 && x < size) {
                            intersections.add(x);
                        }
                    }
                }
            }

            intersections.sort(Integer::compareTo);

            // Fill between pairs of intersections
            for (int i = 0; i < intersections.size() - 1; i += 2) {
                int startX = Math.max(0, intersections.get(i));
                int endX = Math.min(size - 1, intersections.get(i + 1));

                for (int x = startX; x <= endX; x++) {
                    image.setColor(x, y, color);
                }
            }
        }
    }

    private static int getBlockColor(String blockName) {
        // Simple color mapping based on block names - you can expand this
        return switch (blockName.toLowerCase()) {
            case String s when s.contains("grass") -> 0xFF4CAF50;
            case String s when s.contains("dirt") -> 0xFF8D6E63;
            case String s when s.contains("stone") -> 0xFF757575;
            case String s when s.contains("wood") -> 0xFF795548;
            case String s when s.contains("log") -> 0xFF5D4037;
            case String s when s.contains("sand") -> 0xFFFDD835;
            case String s when s.contains("water") -> 0xFF2196F3;
            case String s when s.contains("lava") -> 0xFFFF5722;
            case String s when s.contains("leaves") -> 0xFF2E7D32;
            case String s when s.contains("ore") -> 0xFF37474F;
            case String s when s.contains("snow") -> 0xFFFAFAFA;
            case String s when s.contains("ice") -> 0xFFB3E5FC;
            default -> 0xFF9E9E9E; // Default gray
        };
    }

    private static int adjustBrightness(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        int a = (color >> 24) & 0xFF;

        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    public static class RenderedFace {
        public final int[] xPoints;
        public final int[] yPoints;
        public final float avgDepth;
        public final int color;

        public RenderedFace(int[] xPoints, int[] yPoints, float avgDepth, int color) {
            this.xPoints = xPoints;
            this.yPoints = yPoints;
            this.avgDepth = avgDepth;
            this.color = color;
        }
    }

    public static void markDirty() {
        needsRedraw = true;
    }
}