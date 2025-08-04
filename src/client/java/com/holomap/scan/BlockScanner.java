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

/**
 * Client-side scanner for blocks around the player.
 *
 * Scans a square radius and height band to collect topmost solid blocks suitable for a minimap.
 */
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
                        var data = sampleTopBlocks(world, origin, hr, vr);
                        MinecraftClient.getInstance()
                                .execute(() -> MinimapData.get().setScanResult(origin, hr, data));
                    } catch (Exception e) {
                        HoloMapMod.LOGGER.error("Scan failed", e);
                    }
                });
    }

    /**
     * Returns a dense array of block colors or ids per (x,z) offset. We do top-surface pick: find
     * first non-air/non-fluid from top-down within the vertical range centered on player Y.
     */
    private static int[] sampleTopBlocks(World world, BlockPos origin, int hr, int vr) {
        int size = (2 * hr + 1);
        int[] colors = new int[size * size];

        int yCenter = origin.getY();
        int yTop = Math.min(world.getTopY(), yCenter + vr / 2);
        int yBottom = Math.max(world.getBottomY(), yCenter - vr / 2);

        // Pre-warm chunk cache by accessing chunk references for the area
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

                // sample top-down
                int color = 0x00000000;
                for (int y = yTop; y >= yBottom; y--) {
                    BlockState state = chunk.getBlockState(new BlockPos(absX, y, absZ));
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        color = approximateColor(state);
                        break;
                    }
                }

                int ix = (dz + hr) * size + (dx + hr);
                colors[ix] = color;
            }
        }

        return colors;
    }

    /**
     * Very rough color by block. For better results, consider using the block color provider for the
     * biome or a precomputed palette.
     */
    private static int approximateColor(BlockState state) {
        var id = Registries.BLOCK.getId(state.getBlock()).toString();
        // Simple heuristics
        if (id.contains("grass") || id.contains("leaves")) return 0xFF3DAA48;
        if (id.contains("stone") || id.contains("deepslate") || id.contains("andesite"))
            return 0xFF7A7A7A;
        if (id.contains("sand")) return 0xFFE7DF9A;
        if (id.contains("water")) return 0xFF2F66B3;
        if (id.contains("snow")) return 0xFFFFFFFF;
        if (id.contains("dirt") || id.contains("mud")) return 0xFF8B5A3C;
        if (id.contains("wood") || id.contains("log") || id.contains("planks")) return 0xFFB58A59;
        return 0xFF8F8F8F;
    }
}