package com.holomap.scan;

import com.holomap.HoloMapMod;
import com.holomap.map.MinimapData3D;
import com.holomap.render.BlockColorProvider;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;

public final class BlockScanner3D {
    private BlockScanner3D() {}

    public static void scanAroundPlayerAsync(
            net.minecraft.entity.player.PlayerEntity player, int horizontalRadius, int verticalRange) {
        if (player == null || player.getWorld() == null) return;

        final var world = player.getWorld();
        final var origin = player.getBlockPos();
        final int hr = MathHelper.clamp(horizontalRadius, 8, 32);
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
        public final BlockColorProvider.BlockInfo blockInfo;
        public final int surfaceDistance; // Distance from surface level

        public Block3DData(BlockState state, BlockPos pos, boolean[] visibleFaces, BlockColorProvider.BlockInfo blockInfo, int surfaceDistance) {
            this.state = state;
            this.pos = pos;
            this.visibleFaces = visibleFaces;
            this.blockInfo = blockInfo;
            this.surfaceDistance = surfaceDistance;
        }
    }

    private static Block3DData[][][] scan3DTerrain(World world, BlockPos origin, int hr, int vr) {
        int size = 2 * hr + 1;
        int height = vr;
        Block3DData[][][] blocks = new Block3DData[size][height][size];

        // Start from surface and go down
        int yStart = origin.getY() + 5;
        int yEnd = Math.max(world.getBottomY(), origin.getY() - vr + 5);

        for (int dx = -hr; dx <= hr; dx++) {
            for (int dz = -hr; dz <= hr; dz++) {
                // Get surface level for proper underground filtering
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, origin.getX() + dx, origin.getZ() + dz);
                scanColumn(world, origin.getX() + dx, origin.getZ() + dz, yStart, yEnd, blocks, dx + hr, dz + hr, vr, surfaceY);
            }
        }

        return blocks;
    }

    private static void scanColumn(World world, int x, int z, int yStart, int yEnd, Block3DData[][][] blocks, int arrayX, int arrayZ, int maxHeight, int surfaceY) {
        for (int y = yStart; y >= yEnd && (yStart - y) < maxHeight; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            BlockColorProvider.BlockInfo blockInfo = BlockColorProvider.getBlockInfo(state);

            int surfaceDistance = Math.abs(y - surfaceY);

            // Only skip air if it's not important structurally - now with surface awareness
            if (state.isAir() && !shouldIncludeAir(world, pos, surfaceY)) {
                continue;
            }

            int arrayY = yStart - y;
            if (arrayY >= 0 && arrayY < maxHeight) {
                boolean[] visibleFaces = calculateSmartVisibility(world, pos, state, blockInfo);
                blocks[arrayX][arrayY][arrayZ] = new Block3DData(state, pos, visibleFaces, blockInfo, surfaceDistance);
            }
        }
    }

    private static boolean shouldIncludeAir(World world, BlockPos pos, int surfaceY) {
        // Strict surface-only air inclusion to prevent cave rendering

        // Don't include air blocks that are significantly underground
        int depthBelowSurface = surfaceY - pos.getY();
        if (depthBelowSurface > 8) {
            return false; // Too deep underground, likely a cave
        }

        // For blocks near or above surface, check if they have sky access
        if (depthBelowSurface <= 0) {
            // Above or at surface level - include if it has sky visibility
            return world.isSkyVisible(pos);
        }

        // For shallow underground air (1-8 blocks below surface)
        // Only include if it's part of surface features like overhangs or shallow caves
        if (depthBelowSurface <= 3) {
            // Check if this air block is connected to the surface
            for (int dy = 1; dy <= depthBelowSurface + 2; dy++) {
                BlockPos checkPos = pos.up(dy);
                if (world.isSkyVisible(checkPos)) {
                    return true; // Connected to surface
                }
            }
        }

        return false; // Underground air block, likely a cave
    }

    private static boolean[] calculateSmartVisibility(World world, BlockPos pos, BlockState currentState, BlockColorProvider.BlockInfo currentInfo) {
        boolean[] faces = new boolean[6]; // down, up, north, south, west, east

        BlockPos[] neighbors = {
                pos.down(), pos.up(), pos.north(), pos.south(), pos.west(), pos.east()
        };

        for (int i = 0; i < 6; i++) {
            BlockState neighborState = world.getBlockState(neighbors[i]);
            BlockColorProvider.BlockInfo neighborInfo = BlockColorProvider.getBlockInfo(neighborState);

            faces[i] = shouldShowFace(currentState, currentInfo, neighborState, neighborInfo, i);
        }

        return faces;
    }

    private static boolean shouldShowFace(BlockState current, BlockColorProvider.BlockInfo currentInfo,
                                          BlockState neighbor, BlockColorProvider.BlockInfo neighborInfo, int faceIndex) {

        // Always show faces adjacent to air
        if (neighbor.isAir()) {
            return true;
        }

        // Always show top faces for better 3D appearance
        if (faceIndex == 1) { // Up face
            return true;
        }

        // Show faces between different block types with different priorities
        if (currentInfo.type != neighborInfo.type) {
            return true;
        }

        // Show faces where there's a priority difference (e.g., leaves over grass)
        if (Math.abs(currentInfo.priority - neighborInfo.priority) > 10) {
            return true;
        }

        // Show faces for transparent blocks
        if (currentInfo.isTransparent && !neighborInfo.isTransparent) {
            return true;
        }

        // Show faces for fluids
        if (currentInfo.isFluid && !neighborInfo.isFluid) {
            return true;
        }

        // Show faces between blocks of significantly different colors
        int colorDiff = calculateColorDifference(currentInfo.color, neighborInfo.color);
        if (colorDiff > 50) { // Threshold for color difference
            return true;
        }

        return false;
    }

    private static int calculateColorDifference(int color1, int color2) {
        int r1 = (color1 >>> 16) & 0xFF, g1 = (color1 >>> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >>> 16) & 0xFF, g2 = (color2 >>> 8) & 0xFF, b2 = color2 & 0xFF;

        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }
}