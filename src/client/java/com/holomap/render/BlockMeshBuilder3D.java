package com.holomap.render;

import com.holomap.map.MinimapData3D;
import com.holomap.scan.BlockScanner3D;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public final class BlockMeshBuilder3D {

    public static class BlockVertex {
        public final float x, y, z;    // Position
        public final float u, v;       // Texture coordinates
        public final float r, g, b, a; // Color

        public BlockVertex(float x, float y, float z, float u, float v, float r, float g, float b, float a) {
            this.x = x; this.y = y; this.z = z;
            this.u = u; this.v = v;
            this.r = r; this.g = g; this.b = b; this.a = a;
        }
    }

    public static class BlockMesh {
        public final List<BlockVertex> vertices = new ArrayList<>();
        public final List<Integer> indices = new ArrayList<>();

        public void addQuad(BlockVertex v1, BlockVertex v2, BlockVertex v3, BlockVertex v4) {
            int base = vertices.size();
            vertices.add(v1);
            vertices.add(v2);
            vertices.add(v3);
            vertices.add(v4);

            // Two triangles forming a quad
            indices.add(base); indices.add(base + 1); indices.add(base + 2);
            indices.add(base); indices.add(base + 2); indices.add(base + 3);
        }
    }

    public static BlockMesh buildMesh() {
        var data = MinimapData3D.get();
        if (!data.isReady()) return new BlockMesh();

        BlockMesh mesh = new BlockMesh();
        var blocks = data.blocks();
        var origin = data.origin();
        int hr = data.horizontalRadius();
        int vr = data.verticalRange();

        var client = MinecraftClient.getInstance();

        for (int x = 0; x < blocks.length; x++) {
            for (int y = 0; y < blocks[x].length; y++) {
                for (int z = 0; z < blocks[x][y].length; z++) {
                    var blockData = blocks[x][y][z];
                    if (blockData == null) continue;

                    // World position (centered on origin)
                    float worldX = x - hr;
                    float worldY = y - vr/2f;
                    float worldZ = z - hr;

                    addBlockToMesh(mesh, blockData, worldX, worldY, worldZ, client);
                }
            }
        }

        return mesh;
    }

    private static void addBlockToMesh(BlockMesh mesh, BlockScanner3D.Block3DData blockData,
                                       float x, float y, float z,
                                       MinecraftClient client) {
        BlockState state = blockData.state;
        boolean[] visibleFaces = blockData.visibleFaces;

        BakedModel model = client.getBlockRenderManager().getModel(state);

        // Get texture for each face
        Direction[] directions = {Direction.DOWN, Direction.UP, Direction.NORTH,
                Direction.SOUTH, Direction.WEST, Direction.EAST};

        for (int i = 0; i < 6; i++) {
            if (!visibleFaces[i]) continue; // Skip hidden faces

            Direction face = directions[i];
            Sprite sprite = getBlockFaceSprite(model, state, face);

            addBlockFace(mesh, x, y, z, face, sprite);
        }
    }

    private static Sprite getBlockFaceSprite(BakedModel model, BlockState state, Direction face) {
        var quads = model.getQuads(state, face, MinecraftClient.getInstance().world.random);
        if (!quads.isEmpty()) {
            return quads.get(0).getSprite();
        }

        // Fallback to particle texture
        return model.getParticleSprite();
    }

    private static void addBlockFace(BlockMesh mesh, float x, float y, float z, Direction face, Sprite sprite) {
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        // Simple white lighting for now
        float r = 1f, g = 1f, b = 1f, a = 1f;

        // Apply simple directional lighting
        float lightLevel = getLightLevel(face);
        r *= lightLevel; g *= lightLevel; b *= lightLevel;

        switch (face) {
            case UP -> {
                mesh.addQuad(
                        new BlockVertex(x, y + 1, z, minU, minV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z, maxU, minV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z + 1, maxU, maxV, r, g, b, a),
                        new BlockVertex(x, y + 1, z + 1, minU, maxV, r, g, b, a)
                );
            }
            case DOWN -> {
                mesh.addQuad(
                        new BlockVertex(x, y, z + 1, minU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y, z + 1, maxU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y, z, maxU, minV, r, g, b, a),
                        new BlockVertex(x, y, z, minU, minV, r, g, b, a)
                );
            }
            case NORTH -> {
                mesh.addQuad(
                        new BlockVertex(x + 1, y, z, maxU, maxV, r, g, b, a),
                        new BlockVertex(x, y, z, minU, maxV, r, g, b, a),
                        new BlockVertex(x, y + 1, z, minU, minV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z, maxU, minV, r, g, b, a)
                );
            }
            case SOUTH -> {
                mesh.addQuad(
                        new BlockVertex(x, y, z + 1, minU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y, z + 1, maxU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z + 1, maxU, minV, r, g, b, a),
                        new BlockVertex(x, y + 1, z + 1, minU, minV, r, g, b, a)
                );
            }
            case WEST -> {
                mesh.addQuad(
                        new BlockVertex(x, y, z, minU, maxV, r, g, b, a),
                        new BlockVertex(x, y, z + 1, maxU, maxV, r, g, b, a),
                        new BlockVertex(x, y + 1, z + 1, maxU, minV, r, g, b, a),
                        new BlockVertex(x, y + 1, z, minU, minV, r, g, b, a)
                );
            }
            case EAST -> {
                mesh.addQuad(
                        new BlockVertex(x + 1, y, z + 1, minU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y, z, maxU, maxV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z, maxU, minV, r, g, b, a),
                        new BlockVertex(x + 1, y + 1, z + 1, minU, minV, r, g, b, a)
                );
            }
        }
    }

    private static float getLightLevel(Direction face) {
        return switch (face) {
            case UP -> 1.0f;           // Full brightness on top
            case DOWN -> 0.5f;         // Darker on bottom
            case NORTH, SOUTH -> 0.8f; // Medium brightness on sides
            case WEST, EAST -> 0.6f;   // Slightly darker on other sides
        };
    }
}