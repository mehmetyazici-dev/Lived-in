package dev.livedin.scene;

/** Twelve-second visual activity: prepare, cast, wait, retrieve, rest. No fishing loot is generated. */
public final class FishingCycle {
    public static final int LENGTH = 240;
    public static final int CAST_START = 20, WAIT_START = 40, RETRIEVE_START = 200, REST_START = 220;
    private FishingCycle() {}
    public static boolean hasBobber(int tick) { return tick >= CAST_START && tick < REST_START; }
    public static float extension(float tick) {
        if (tick < WAIT_START) return net.minecraft.util.Mth.clamp((tick - CAST_START) / 20, 0, 1);
        if (tick >= RETRIEVE_START) return net.minecraft.util.Mth.clamp((REST_START - tick) / 20, 0, 1);
        return 1;
    }
    public static float armPitch(float tick) {
        if (tick < 0) return -0.6F;
        if (tick < CAST_START) return -0.8F - 1.0F * tick / CAST_START;
        if (tick < WAIT_START) return -1.8F + (tick - CAST_START) / 20;
        if (tick >= RETRIEVE_START && tick < REST_START) return -0.8F - 0.6F * (tick - RETRIEVE_START) / 20;
        return -0.8F;
    }
}
