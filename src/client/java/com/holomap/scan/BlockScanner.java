package com.holomap.scan;

import com.holomap.HoloMapMod;
import com.holomap.map.MinimapData;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public final class BlockScanner {
    private BlockScanner() {}

    public static void scanAroundPlayerAsync(
            net.minecraft.entity.player.PlayerEntity player, int horizontalRadius, int verticalRange) {
        if (player == null || player.getWorld() == null) return;
        final var world = player.getWorld();
        final var origin = player.getBlockPos();
        final int hr = MathHelper.clamp(horizontalRadius, 8, 128);
        final int vr = MathHelper.clamp(verticalRange, 16, 256);

        CompletableFuture.runAsync(
                () -> {
                    try {
                        var data = sampleTopBlocksWithHeight(world, origin, hr, vr);
                        MinecraftClient.getInstance()
                                .execute(() -> MinimapData.get().setScanResultWithHeight(origin, hr, data.colors, data.heights));
                    } catch (Exception e) {
                        HoloMapMod.LOGGER.error("Scan failed", e);
                    }
                });
    }

    public static class ScanResult {
        public final int[] colors;
        public final int[] heights;

        public ScanResult(int[] colors, int[] heights) {
            this.colors = colors;
            this.heights = heights;
        }
    }

    private static ScanResult sampleTopBlocksWithHeight(World world, BlockPos origin, int hr, int vr) {
        int size = (2 * hr + 1);
        int[] colors = new int[size * size];
        int[] heights = new int[size * size];

        int yCenter = origin.getY();
        int yTop = Math.min(world.getTopY(), yCenter + vr / 2);
        int yBottom = Math.max(world.getBottomY(), yCenter - vr / 2);

        // Pre-warm chunk cache
        int chunkMinX = (origin.getX() - hr) >> 4;
        int chunkMaxX = (origin.getX() + hr) >> 4;
        int chunkMinZ = (origin.getZ() - hr) >> 4;
        int chunkMaxZ = (origin.getZ() + hr) >> 4;

        Chunk[][] chunks = new Chunk[chunkMaxX - chunkMinX + 1][chunkMaxZ - chunkMinZ + 1];
        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                chunks[cx - chunkMinX][cz - chunkMinZ] = world.getChunk(cx, cz);
            }
        }

        for (int dz = -hr; dz <= hr; dz++) {
            for (int dx = -hr; dx <= hr; dx++) {
                int absX = origin.getX() + dx;
                int absZ = origin.getZ() + dz;

                int cx = absX >> 4;
                int cz = absZ >> 4;
                Chunk chunk = chunks[cx - chunkMinX][cz - chunkMinZ];

                int color = 0x00000000;
                int height = yBottom;

                // Sample top-down for surface
                for (int y = yTop; y >= yBottom; y--) {
                    BlockState state = chunk.getBlockState(new BlockPos(absX, y, absZ));
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        color = approximateColorWithHeight(state, y - yCenter);
                        height = y;
                        break;
                    }
                }

                int ix = (dz + hr) * size + (dx + hr);
                colors[ix] = color;
                heights[ix] = height;
            }
        }

        return new ScanResult(colors, heights);
    }

    private static int approximateColorWithHeight(BlockState state, int relativeHeight) {
        var id = Registries.BLOCK.getId(state.getBlock()).toString();
        int baseColor = getBaseBlockColor(id);

        // Add height-based lighting (simple ambient occlusion approximation)
        float heightFactor = MathHelper.clamp(relativeHeight / 32.0f + 0.5f, 0.3f, 1.0f);

        int a = (baseColor >>> 24) & 0xFF;
        int r = (int) (((baseColor >>> 16) & 0xFF) * heightFactor);
        int g = (int) (((baseColor >>> 8) & 0xFF) * heightFactor);
        int b = (int) ((baseColor & 0xFF) * heightFactor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getBaseBlockColor(String id) {
        if (id.contains("grass") || id.contains("leaves")) return 0xFF3DAA48;
        if (id.contains("stone") || id.contains("deepslate") || id.contains("andesite")) return 0xFF7A7A7A;
        if (id.contains("sand")) return 0xFFE7DF9A;
        if (id.contains("water")) return 0xFF2F66B3;
        if (id.contains("snow")) return 0xFFFFFFFF;
        if (id.contains("dirt") || id.contains("mud")) return 0xFF8B5A3C;
        if (id.contains("wood") || id.contains("log") || id.contains("planks")) return 0xFFB58A59;
        return 0xFF8F8F8F;
    }
}