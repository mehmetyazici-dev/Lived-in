package dev.livedin.scene;

/** An original short phrase using vanilla harp timbre; -1 represents a rest. */
public final class CampfireMelody {
    public static final int TICKS_PER_NOTE = 10;
    private static final int[] NOTES = {12, 16, 19, 16, 14, 17, 21, 17, 12, 19, 16, 12, -1, -1, -1, -1};

    private CampfireMelody() {}
    public static int noteAt(long gameTime) {
        return NOTES[(int) Math.floorMod(gameTime / TICKS_PER_NOTE, NOTES.length)];
    }
    public static float pitch(int note) {
        return (float) Math.pow(2.0, (note - 12) / 12.0);
    }
}