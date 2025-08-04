package com.holomap.scan;

import com.holomap.HoloMapMod;
import com.holomap.map.MinimapData3D;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class BlockScanner3D {
    private BlockScanner3D() {}

    public static void scanAroundPlayerAsync(
            net.minecraft.entity.player.PlayerEntity player, int horizontalRadius, int verticalRange) {
        if (player == null || player.getWorld() == null) return;

        final var world = player.getWorld();
        final var origin = player.getBlockPos();
        final int hr = MathHelper.clamp(horizontalRadius, 8, 32); // Smaller for performance
        final int vr = MathHelper.clamp(verticalRange, 8, 32);

        CompletableFuture.runAsync(() -> {
            try {
                var data = scan3DTerrain(world, origin, hr, vr);
                MinecraftClient.getInstance().execute(() ->
                        MinimapData3D.get().setScanResult(origin, hr, vr, data));
            } catch (Exception e) {
                HoloMapMod.LOGGER.error("3D Scan failed", e);
            }
        });
    }

    public static class Block3DData {
        public final BlockState state;
        public final BlockPos pos;
        public final boolean[] visibleFaces; // [down, up, north, south, west, east]

        public Block3DData(BlockState state, BlockPos pos, boolean[] visibleFaces) {
            this.state = state;
            this.pos = pos;
            this.visibleFaces = visibleFaces;
        }
    }

    private static Block3DData[][][] scan3DTerrain(World world, BlockPos origin, int hr, int vr) {
        int size = 2 * hr + 1;
        int height = vr;
        Block3DData[][][] blocks = new Block3DData[size][height][size];

        // Start from surface and go down
        int yStart = origin.getY() + 5; // Start above player
        int yEnd = Math.max(world.getBottomY(), origin.getY() - vr + 5);

        for (int dx = -hr; dx <= hr; dx++) {
            for (int dz = -hr; dz <= hr; dz++) {
                // Find surface level first
                int surfaceY = findSurfaceLevel(world, origin.getX() + dx, origin.getZ() + dz, yStart, yEnd);

                // Scan from surface down to limited depth
                int scanDepth = Math.min(vr, 16); // Limit depth for performance
                for (int dy = 0; dy < scanDepth; dy++) {
                    int y = surfaceY - dy;
                    if (y < yEnd) break;

                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    BlockState state = world.getBlockState(pos);

                    // Skip air blocks
                    if (state.isAir()) {
                        // But if we're at surface level and it's air, still record it
                        if (dy > 3) continue; // Only skip air after going down a bit
                    }

                    boolean[] visibleFaces = calculateVisibleFaces(world, pos);
                    blocks[dx + hr][dy][dz + hr] = new Block3DData(state, pos, visibleFaces);
                }
            }
        }

        return blocks;
    }

    private static int findSurfaceLevel(World world, int x, int z, int startY, int endY) {
        // Find the first solid block from top down
        for (int y = startY; y >= endY; y--) {
            BlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return endY; // Fallback to bottom if no surface found
    }

    private static boolean[] calculateVisibleFaces(World world, BlockPos pos) {
        boolean[] faces = new boolean[6]; // down, up, north, south, west, east

        BlockPos[] neighbors = {
                pos.down(), pos.up(), pos.north(), pos.south(), pos.west(), pos.east()
        };

        for (int i = 0; i < 6; i++) {
            BlockState neighbor = world.getBlockState(neighbors[i]);
            // Face is visible if neighbor is air or transparent
            faces[i] = neighbor.isAir() || !neighbor.isOpaque();
        }

        return faces;
    }
}