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
        if (cachedTexture != null) {
            cachedTexture.close();
        }

        NativeImage image = new NativeImage(size, size, false);

        // Clear to transparent
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setColor(x, y, 0x00000000);
            }
        }

        renderToImage(image, size);

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

        // Sort by priority first, then depth
        faces.sort((f1, f2) -> {
            int priorityCompare = Integer.compare(f1.priority, f2.priority);
            if (priorityCompare != 0) return priorityCompare;
            return Double.compare(f2.avgDepth, f1.avgDepth); // Higher depth first
        });

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
                    if (blockData == null) continue;

                    // Use consistent world coordinates relative to center - like original but centered
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
        BlockColorProvider.BlockInfo blockInfo = blockData.blockInfo;

        // Skip air blocks unless they're structurally important
        if (blockInfo.type == BlockColorProvider.BlockType.AIR) {
            return;
        }

        // Filter out deep underground blocks to maintain clean surface representation
        if (blockData.surfaceDistance > 12 && blockInfo.priority < 70) {
            return;
        }

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

        // Use block color from BlockColorProvider
        int baseColor = blockInfo.color;

        // Different lighting for each face
        float[] lightLevels = {0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f}; // down, up, north, south, west, east

        for (int i = 0; i < 6; i++) {
            if (!visibleFaces[i]) continue;

            Vector3f[] screenPoints = new Vector3f[4];
            float totalDepth = 0;
            boolean allInFront = true;

            for (int j = 0; j < 4; j++) {
                float[] vertex = faceVertices[i][j];
                screenPoints[j] = camera.projectToScreen(vertex[0], vertex[1], vertex[2], viewSize);
                totalDepth += screenPoints[j].z;

                if (screenPoints[j].z > 1.0f) {
                    allInFront = false;
                    break;
                }
            }

            if (!allInFront) continue;

            int[] xPoints = new int[4];
            int[] yPoints = new int[4];

            for (int j = 0; j < 4; j++) {
                xPoints[j] = Math.round(screenPoints[j].x);
                yPoints[j] = Math.round(screenPoints[j].y);
            }

            float avgDepth = totalDepth / 4.0f;
            int faceColor = BlockColorProvider.adjustBrightness(baseColor, lightLevels[i]);

            faces.add(new RenderedFace(xPoints, yPoints, avgDepth, faceColor, blockInfo.priority));
        }
    }

    private static void drawPolygonToImage(NativeImage image, int[] xPoints, int[] yPoints, int color, int size) {
        if (xPoints.length != 4) return;

        int minX = Math.max(0, Math.min(Math.min(Math.min(xPoints[0], xPoints[1]), xPoints[2]), xPoints[3]));
        int maxX = Math.min(size - 1, Math.max(Math.max(Math.max(xPoints[0], xPoints[1]), xPoints[2]), xPoints[3]));
        int minY = Math.max(0, Math.min(Math.min(Math.min(yPoints[0], yPoints[1]), yPoints[2]), yPoints[3]));
        int maxY = Math.min(size - 1, Math.max(Math.max(Math.max(yPoints[0], yPoints[1]), yPoints[2]), yPoints[3]));

        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();

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

            for (int i = 0; i < intersections.size() - 1; i += 2) {
                int startX = Math.max(0, intersections.get(i));
                int endX = Math.min(size - 1, intersections.get(i + 1));

                for (int x = startX; x <= endX; x++) {
                    // Only overwrite if current pixel is transparent or lower priority
                    int existingColor = image.getColor(x, y);
                    if ((existingColor & 0xFF000000) == 0 || shouldOverwrite(existingColor, color)) {
                        image.setColor(x, y, color);
                    }
                }
            }
        }
    }

    private static boolean shouldOverwrite(int existingColor, int newColor) {
        // Simple overwrite logic - can be enhanced
        int existingAlpha = (existingColor >>> 24) & 0xFF;
        int newAlpha = (newColor >>> 24) & 0xFF;

        return newAlpha >= existingAlpha;
    }

    public static class RenderedFace {
        public final int[] xPoints;
        public final int[] yPoints;
        public final float avgDepth;
        public final int color;
        public final int priority;

        public RenderedFace(int[] xPoints, int[] yPoints, float avgDepth, int color, int priority) {
            this.xPoints = xPoints;
            this.yPoints = yPoints;
            this.avgDepth = avgDepth;
            this.color = color;
            this.priority = priority;
        }
    }

    public static void markDirty() {
        needsRedraw = true;
    }
}