package itamio.coldsnow.world;

import itamio.coldsnow.core.SnowDepthPolicy;
import itamio.coldsnow.core.StormSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * Re-implements vanilla snow piling without a mixin and without a custom
 * block. Vanilla piling is disabled by the {@code snowAccumulationHeight=0}
 * game rule; this service uses the very same vanilla blocks ({@link Blocks#SNOW}
 * layers 1..8 and {@link Blocks#SNOW_BLOCK} full cubes) to grow and melt snow.
 *
 * <p>Work is throttled: every {@code intervalTicks} a small rotating slice of
 * the 17x17 columns around one round-robin player is processed.</p>
 */
public final class AccumulationService {
    private static final int VANILLA_MAX_LAYERS = 8;
    private static final int LAYERS_PER_BLOCK = 8;
    private static final int MAX_LAYERS_CAP = 64;
    private static final int COLUMNS_PER_TICK = 48;
    private static final int GRID_RADIUS = 8;
    private static final int GRID_SIZE = GRID_RADIUS * 2 + 1;
    private static final int GRID_COLUMNS = GRID_SIZE * GRID_SIZE;
    private static final int MAX_SCAN = 80;

    private final int maxLayers;
    private final int intervalTicks;
    private final SnowDepthPolicy policy = new SnowDepthPolicy();

    private int tickCounter;
    private int columnCursor;
    private int playerCursor;

    public AccumulationService(int maxLayers, int intervalTicks) {
        this.maxLayers = Math.max(VANILLA_MAX_LAYERS, Math.min(MAX_LAYERS_CAP, maxLayers));
        this.intervalTicks = Math.max(1, intervalTicks);
    }

    public void tick(ServerLevel level, StormSnapshot snapshot) {
        if (level == null || snapshot == null) {
            return;
        }
        if (++this.tickCounter < this.intervalTicks) {
            return;
        }
        this.tickCounter = 0;

        List<ServerPlayer> players = level.players();
        if (players == null || players.isEmpty()) {
            return;
        }
        ServerPlayer player;
        try {
            player = players.get(Math.floorMod(this.playerCursor++, players.size()));
        } catch (RuntimeException ignored) {
            return;
        }
        if (player == null || !player.isAlive()) {
            return;
        }

        BlockPos center = player.blockPosition();
        int target;
        try {
            target = this.policy.targetLayers(snapshot, this.maxLayers);
        } catch (RuntimeException ignored) {
            return;
        }

        for (int processed = 0; processed < COLUMNS_PER_TICK; processed++) {
            int index = Math.floorMod(this.columnCursor++, GRID_COLUMNS);
            int dx = (index % GRID_SIZE) - GRID_RADIUS;
            int dz = (index / GRID_SIZE) - GRID_RADIUS;
            BlockPos ground = findGround(level, center.getX() + dx, center.getZ() + dz);
            if (ground == null) {
                continue;
            }
            int current = measureDepth(level, ground);
            final int step;
            try {
                step = this.policy.step(current, target);
            } catch (RuntimeException ignored) {
                continue;
            }

            if (step > 0) {
                if (current >= this.maxLayers) {
                    continue;
                }
                // Only accumulate where the biome actually permits snowfall.
                // The check happens at the real placement position inside addLayer.
                addLayer(level, ground);
            } else if (step < 0 && current > 0) {
                // Melting is allowed even if the biome has since warmed up.
                removeLayer(level, ground);
            }
        }
    }

    /** Finds the highest non-air, non-snow block in a column (the ground). */
    private static BlockPos findGround(ServerLevel level, int x, int z) {
        try {
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            y = Math.min(y, level.getMaxBuildHeight() - 1);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int scanned = 0; scanned < MAX_SCAN && y >= level.getMinBuildHeight(); scanned++, y--) {
                BlockState state = level.getBlockState(cursor.set(x, y, z));
                if (state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                    continue;
                }
                return new BlockPos(x, y, z);
            }
        } catch (RuntimeException ignored) {
            // Unloaded or out-of-bounds columns are simply skipped.
        }
        return null;
    }

    /** Total snow depth measured in vanilla snow-layer units. */
    private static int measureDepth(Level level, BlockPos ground) {
        int depth = 0;
        int y = ground.getY() + 1;
        int maxY = level.getMaxBuildHeight() - 1;
        for (int scanned = 0; scanned < MAX_SCAN && y <= maxY; scanned++, y++) {
            BlockState state = level.getBlockState(new BlockPos(ground.getX(), y, ground.getZ()));
            if (state.is(Blocks.SNOW_BLOCK)) {
                depth += LAYERS_PER_BLOCK;
            } else if (state.is(Blocks.SNOW)) {
                depth += state.getValue(SnowLayerBlock.LAYERS);
            } else {
                break;
            }
        }
        return depth;
    }

    /** Adds exactly one layer of depth to the column above {@code ground}. */
    private static void addLayer(ServerLevel level, BlockPos ground) {
        BlockPos topPos = null;
        BlockState topState = null;
        int y = ground.getY() + 1;
        int maxY = level.getMaxBuildHeight() - 1;
        for (int scanned = 0; scanned < MAX_SCAN && y <= maxY; scanned++, y++) {
            BlockPos pos = new BlockPos(ground.getX(), y, ground.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                break;
            }
            if (!state.is(Blocks.SNOW) && !state.is(Blocks.SNOW_BLOCK)) {
                break;
            }
            topPos = pos;
            topState = state;
        }

        if (topPos == null || topState == null) {
            if (canSnowAt(level, ground.above())) {
                setSnowState(level, ground.above(), oneLayer());
            }
            return;
        }
        if (topState.is(Blocks.SNOW)) {
            int layers = topState.getValue(SnowLayerBlock.LAYERS);
            if (layers < VANILLA_MAX_LAYERS) {
                if (canSnowAt(level, topPos)) {
                    setSnowState(level, topPos, topState.setValue(SnowLayerBlock.LAYERS, layers + 1));
                }
            } else if (canSnowAt(level, topPos) && canSnowAt(level, topPos.above())) {
                // 8-layer snow -> full cube is layer-neutral, so add a fresh
                // layer above it to actually gain the next layer of depth.
                setSnowState(level, topPos, Blocks.SNOW_BLOCK.defaultBlockState());
                setSnowState(level, topPos.above(), oneLayer());
            }
        } else if (canSnowAt(level, topPos.above())) {
            // A full cube is on top; start a new layer stack above it.
            setSnowState(level, topPos.above(), oneLayer());
        }
    }

    /** Mirrors the vanilla biome gate, checked at the exact placement position. */
    private static boolean canSnowAt(ServerLevel level, BlockPos pos) {
        try {
            Biome biome = level.getBiome(pos).value();
            return biome != null && biome.shouldSnow(level, pos);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Removes exactly one layer of depth from the column above {@code ground}. */
    private static void removeLayer(ServerLevel level, BlockPos ground) {
        BlockPos topPos = null;
        BlockState topState = null;
        int y = ground.getY() + 1;
        int maxY = level.getMaxBuildHeight() - 1;
        for (int scanned = 0; scanned < MAX_SCAN && y <= maxY; scanned++, y++) {
            BlockPos pos = new BlockPos(ground.getX(), y, ground.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW_BLOCK)) {
                topPos = pos;
                topState = state;
            } else if (state.is(Blocks.SNOW)) {
                topPos = pos;
                topState = state;
            } else {
                break;
            }
        }
        if (topPos == null || topState == null) {
            return;
        }
        if (topState.is(Blocks.SNOW)) {
            int layers = topState.getValue(SnowLayerBlock.LAYERS);
            if (layers > 1) {
                setSnowState(level, topPos, topState.setValue(SnowLayerBlock.LAYERS, layers - 1));
            } else {
                setSnowState(level, topPos, Blocks.AIR.defaultBlockState());
            }
        } else {
            // Peel one layer at a time: a full cube becomes a 7-layer snow
            // stack, so melting from deep drifts is gradual like vanilla.
            setSnowState(level, topPos, Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, VANILLA_MAX_LAYERS - 1));
        }
    }

    private static BlockState oneLayer() {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
    }

    private static void setSnowState(ServerLevel level, BlockPos pos, BlockState newState) {
        if (pos == null || newState == null || pos.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        BlockState oldState = level.getBlockState(pos);
        if (oldState.equals(newState)) {
            return;
        }
        // Push entities up when the collision shape grows, exactly as vanilla does.
        Block.pushEntitiesUp(oldState, newState, level, pos);
        level.setBlockAndUpdate(pos, newState);
    }

    private AccumulationService() {
        // Use the public constructor.
        this(VANILLA_MAX_LAYERS, 1);
    }
}
