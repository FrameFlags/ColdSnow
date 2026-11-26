package itamio.coldsnow.core;

/** Weather phases of a Cold Snow cycle. {@code CLEAR} is the "no storm" sentinel. */
public enum StormStage {
    CLEAR,
    LIGHT,
    HEAVY,
    BLIZZARD,
    TAPER
}
