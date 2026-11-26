package itamio.coldsnow.core;

/**
 * Immutable view of the storm state at one world tick.
 *
 * @param stage            the active stage (never {@link StormStage#CLEAR} for a computed timeline)
 * @param cycleIndex       index of the cycle containing {@code dayTimeTicks}
 * @param dayTimeTicks     the queried world time
 * @param tickInStage      ticks elapsed since the start of the stage, inclusive from 0
 * @param stageLengthTicks total length of the current stage in ticks
 * @param intensity        interpolated snow intensity in [0, 1]
 */
public record StormSnapshot(StormStage stage, int cycleIndex, long dayTimeTicks,
                            long tickInStage, long stageLengthTicks, float intensity) {
}
