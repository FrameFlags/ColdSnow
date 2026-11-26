package itamio.coldsnow.server;

import itamio.coldsnow.core.StormStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent per-world storm bookkeeping. Stored in the overworld's
 * {@code DimensionDataStorage} under {@link #KEY} so the cycle index and the
 * last applied weather stage survive server restarts.
 *
 * <p>Note: Minecraft 1.20.1 does not have a {@code SavedData.Factory} class.
 * {@code DimensionDataStorage.computeIfAbsent} takes a
 * {@code (Function<CompoundTag,T>, Supplier<T>, String)} triple instead.</p>
 */
public final class StormStateData extends SavedData {
    public static final String KEY = "coldsnow-storm";

    public int cycleIndex = -1;
    public StormStage lastStage = StormStage.CLEAR;
    public boolean rulesInitialized = false;

    public static StormStateData create() {
        return new StormStateData();
    }

    public static StormStateData load(CompoundTag tag) {
        StormStateData data = new StormStateData();
        if (tag == null) {
            return data;
        }
        try {
            if (tag.contains("cycleIndex", CompoundTag.TAG_INT)) {
                data.cycleIndex = tag.getInt("cycleIndex");
            }
            if (tag.contains("lastStage", CompoundTag.TAG_STRING)) {
                String name = tag.getString("lastStage");
                try {
                    data.lastStage = StormStage.valueOf(name);
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    data.lastStage = StormStage.CLEAR;
                }
            }
            if (tag.contains("rulesInitialized", CompoundTag.TAG_BYTE)) {
                data.rulesInitialized = tag.getBoolean("rulesInitialized");
            }
        } catch (RuntimeException ignored) {
            // Corrupt NBT must never take the world down - fail open with defaults.
            data.cycleIndex = -1;
            data.lastStage = StormStage.CLEAR;
            data.rulesInitialized = false;
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putInt("cycleIndex", this.cycleIndex);
        tag.putString("lastStage", this.lastStage == null ? StormStage.CLEAR.name() : this.lastStage.name());
        tag.putBoolean("rulesInitialized", this.rulesInitialized);
        return tag;
    }

    /** Loads (or creates) the persistent state in the given level's data storage. */
    public static StormStateData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(StormStateData::load, StormStateData::create, KEY);
    }

    private StormStateData() {
    }
}
