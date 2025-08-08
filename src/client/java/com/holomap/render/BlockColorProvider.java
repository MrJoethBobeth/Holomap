package com.holomap.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKeys;
import java.util.HashMap;
import java.util.Map;

public final class BlockColorProvider {

    public enum BlockType {
        LEAVES,
        WOOD,
        STONE,
        DIRT,
        GRASS,
        WATER,
        LAVA,
        SAND,
        GRAVEL,
        ORE,
        METAL,
        WOOL,
        CONCRETE,
        NETHER,
        END,
        SPECIAL,
        AIR,
        UNKNOWN
    }

    public static class BlockInfo {
        public final int color;
        public final BlockType type;
        public final boolean isTransparent;
        public final boolean isFluid;
        public final int priority; // Higher = renders on top

        public BlockInfo(int color, BlockType type, boolean isTransparent, boolean isFluid, int priority) {
            this.color = color;
            this.type = type;
            this.isTransparent = isTransparent;
            this.isFluid = isFluid;
            this.priority = priority;
        }
    }

    private static final Map<String, BlockInfo> BLOCK_REGISTRY = new HashMap<>();

    static {
        initializeBlockColors();
    }

    private static void initializeBlockColors() {
        // Trees and vegetation (high priority to show above ground)
        addBlock("oak_leaves", 0xFF4F7F00, BlockType.LEAVES, true, false, 100);
        addBlock("spruce_leaves", 0xFF2F5F2F, BlockType.LEAVES, true, false, 100);
        addBlock("birch_leaves", 0xFF7FCC19, BlockType.LEAVES, true, false, 100);
        addBlock("jungle_leaves", 0xFF2F7F2F, BlockType.LEAVES, true, false, 100);
        addBlock("acacia_leaves", 0xFF4F9F4F, BlockType.LEAVES, true, false, 100);
        addBlock("dark_oak_leaves", 0xFF1F4F1F, BlockType.LEAVES, true, false, 100);
        addBlock("mangrove_leaves", 0xFF5F7F1F, BlockType.LEAVES, true, false, 100);
        addBlock("cherry_leaves", 0xFFFFB7C5, BlockType.LEAVES, true, false, 100);

        // Wood types (medium-high priority)
        addBlock("oak_log", 0xFF8F7748, BlockType.WOOD, false, false, 80);
        addBlock("oak_wood", 0xFF8F7748, BlockType.WOOD, false, false, 80);
        addBlock("oak_planks", 0xFF8F7748, BlockType.WOOD, false, false, 80);
        addBlock("spruce_log", 0xFF654321, BlockType.WOOD, false, false, 80);
        addBlock("spruce_wood", 0xFF654321, BlockType.WOOD, false, false, 80);
        addBlock("spruce_planks", 0xFF654321, BlockType.WOOD, false, false, 80);
        addBlock("birch_log", 0xFFF7E9A3, BlockType.WOOD, false, false, 80);
        addBlock("birch_wood", 0xFFF7E9A3, BlockType.WOOD, false, false, 80);
        addBlock("birch_planks", 0xFFF7E9A3, BlockType.WOOD, false, false, 80);
        addBlock("jungle_log", 0xFF976D4D, BlockType.WOOD, false, false, 80);
        addBlock("jungle_wood", 0xFF976D4D, BlockType.WOOD, false, false, 80);
        addBlock("jungle_planks", 0xFF976D4D, BlockType.WOOD, false, false, 80);
        addBlock("acacia_log", 0xFFD87F33, BlockType.WOOD, false, false, 80);
        addBlock("acacia_wood", 0xFFD87F33, BlockType.WOOD, false, false, 80);
        addBlock("acacia_planks", 0xFFD87F33, BlockType.WOOD, false, false, 80);
        addBlock("dark_oak_log", 0xFF3F2F1F, BlockType.WOOD, false, false, 80);
        addBlock("dark_oak_wood", 0xFF3F2F1F, BlockType.WOOD, false, false, 80);
        addBlock("dark_oak_planks", 0xFF3F2F1F, BlockType.WOOD, false, false, 80);
        addBlock("mangrove_log", 0xFF7F4F2F, BlockType.WOOD, false, false, 80);
        addBlock("mangrove_wood", 0xFF7F4F2F, BlockType.WOOD, false, false, 80);
        addBlock("mangrove_planks", 0xFF7F4F2F, BlockType.WOOD, false, false, 80);
        addBlock("cherry_log", 0xFFE8B4B8, BlockType.WOOD, false, false, 80);
        addBlock("cherry_wood", 0xFFE8B4B8, BlockType.WOOD, false, false, 80);
        addBlock("cherry_planks", 0xFFE8B4B8, BlockType.WOOD, false, false, 80);
        addBlock("crimson_stem", 0xFF943F61, BlockType.WOOD, false, false, 80);
        addBlock("crimson_hyphae", 0xFF943F61, BlockType.WOOD, false, false, 80);
        addBlock("crimson_planks", 0xFF943F61, BlockType.WOOD, false, false, 80);
        addBlock("warped_stem", 0xFF3A8E8C, BlockType.WOOD, false, false, 80);
        addBlock("warped_hyphae", 0xFF3A8E8C, BlockType.WOOD, false, false, 80);
        addBlock("warped_planks", 0xFF3A8E8C, BlockType.WOOD, false, false, 80);

        // Fluids (very high priority to show properly)
        addBlock("water", 0xFF4040FF, BlockType.WATER, true, true, 90);
        addBlock("lava", 0xFFFF4500, BlockType.LAVA, false, true, 90);

        // Ground blocks (lower priority)
        addBlock("grass_block", 0xFF7FB238, BlockType.GRASS, false, false, 60);
        addBlock("dirt", 0xFF976D4D, BlockType.DIRT, false, false, 50);
        addBlock("coarse_dirt", 0xFF876D4D, BlockType.DIRT, false, false, 50);
        addBlock("podzol", 0xFF664B33, BlockType.DIRT, false, false, 50);
        addBlock("mycelium", 0xFF7F5F7F, BlockType.DIRT, false, false, 50);

        // Sand and gravel
        addBlock("sand", 0xFFF7E9A3, BlockType.SAND, false, false, 55);
        addBlock("red_sand", 0xFFD87F33, BlockType.SAND, false, false, 55);
        addBlock("gravel", 0xFF707070, BlockType.GRAVEL, false, false, 55);

        // Stone types (low-medium priority)
        addBlock("stone", 0xFF707070, BlockType.STONE, false, false, 40);
        addBlock("cobblestone", 0xFF606060, BlockType.STONE, false, false, 40);
        addBlock("granite", 0xFF976D4D, BlockType.STONE, false, false, 40);
        addBlock("andesite", 0xFF707070, BlockType.STONE, false, false, 40);
        addBlock("diorite", 0xFFEDEDE3, BlockType.STONE, false, false, 40);
        addBlock("deepslate", 0xFF404040, BlockType.STONE, false, false, 40);

        // Ores (medium priority)
        addBlock("coal_ore", 0xFF2F2F2F, BlockType.ORE, false, false, 45);
        addBlock("iron_ore", 0xFF8F7F6F, BlockType.ORE, false, false, 45);
        addBlock("gold_ore", 0xFFFFD700, BlockType.ORE, false, false, 45);
        addBlock("diamond_ore", 0xFF5CDBD5, BlockType.ORE, false, false, 45);
        addBlock("emerald_ore", 0xFF00D93A, BlockType.ORE, false, false, 45);
        addBlock("lapis_ore", 0xFF4A80FF, BlockType.ORE, false, false, 45);
        addBlock("redstone_ore", 0xFFFF0000, BlockType.ORE, false, false, 45);

        // Ice (high priority for visibility)
        addBlock("ice", 0xFFA0A0FF, BlockType.SPECIAL, true, false, 85);
        addBlock("packed_ice", 0xFF9090EE, BlockType.SPECIAL, false, false, 85);
        addBlock("blue_ice", 0xFF8080DD, BlockType.SPECIAL, false, false, 85);

        // Special blocks
        addBlock("snow", 0xFFFAFAFA, BlockType.SPECIAL, false, false, 70);
        addBlock("snow_block", 0xFFFAFAFA, BlockType.SPECIAL, false, false, 70);
        addBlock("obsidian", 0xFF0F0F0F, BlockType.SPECIAL, false, false, 60);
        addBlock("glowstone", 0xFFFFE64D, BlockType.SPECIAL, false, false, 75);
    }

    private static void addBlock(String namePattern, int color, BlockType type, boolean transparent, boolean fluid, int priority) {
        BLOCK_REGISTRY.put(namePattern, new BlockInfo(color, type, transparent, fluid, priority));
    }

    public static BlockInfo getBlockInfo(BlockState state) {
        if (state.isAir()) {
            return new BlockInfo(0x00000000, BlockType.AIR, true, false, 0);
        }

        Block block = state.getBlock();
        String registryName = "";

        if (MinecraftClient.getInstance().world != null) {
            registryName = MinecraftClient.getInstance().world.getRegistryManager()
                    .get(RegistryKeys.BLOCK)
                    .getId(block).toString();
        }

        // Find matching block info
        for (Map.Entry<String, BlockInfo> entry : BLOCK_REGISTRY.entrySet()) {
            if (registryName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Fallback logic for unknown blocks
        String blockName = registryName.toLowerCase();
        if (blockName.contains("leaves")) {
            return new BlockInfo(0xFF4F7F00, BlockType.LEAVES, true, false, 100);
        }
        if (blockName.contains("log") || blockName.contains("wood")) {
            return new BlockInfo(0xFF8F7748, BlockType.WOOD, false, false, 80);
        }
        if (blockName.contains("water")) {
            return new BlockInfo(0xFF4040FF, BlockType.WATER, true, true, 90);
        }
        if (blockName.contains("lava")) {
            return new BlockInfo(0xFFFF4500, BlockType.LAVA, false, true, 90);
        }
        if (blockName.contains("grass")) {
            return new BlockInfo(0xFF7FB238, BlockType.GRASS, false, false, 60);
        }
        if (blockName.contains("dirt")) {
            return new BlockInfo(0xFF976D4D, BlockType.DIRT, false, false, 50);
        }
        if (blockName.contains("stone")) {
            return new BlockInfo(0xFF707070, BlockType.STONE, false, false, 40);
        }

        // Ultimate fallback
        return new BlockInfo(0xFF9E9E9E, BlockType.UNKNOWN, false, false, 30);
    }

    public static int adjustBrightness(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >>> 16) & 0xFF) * factor);
        int g = (int) (((color >>> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);

        return (a << 24) | (Math.min(255, Math.max(0, r)) << 16) |
                (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, b));
    }
}