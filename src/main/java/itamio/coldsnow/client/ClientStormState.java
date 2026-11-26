package itamio.coldsnow.client;

import itamio.coldsnow.core.StormSnapshot;
import itamio.coldsnow.core.StormStage;

/**
 * Client-side mirror of the last storm snapshot received from the server.
 * {@link #get()} is always non-null and defaults to a CLEAR snapshot.
 */
public final class ClientStormState {
    private static final StormSnapshot CLEAR =
            new StormSnapshot(StormStage.CLEAR, -1, 0L, 0L, 0L, 0f);

    private static volatile StormSnapshot current = CLEAR;

    private ClientStormState() {
    }

    public static void set(StormSnapshot snapshot) {
        current = snapshot == null ? CLEAR : snapshot;
    }

    public static StormSnapshot get() {
        return current;
    }

    /** Explicitly returns to the non-storming default (used on disconnect / CLEAR packets). */
    public static void clear() {
        current = CLEAR;
    }
}
