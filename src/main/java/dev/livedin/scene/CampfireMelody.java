package dev.livedin.scene;

/** The supplied twelve-bar guitar melody, in concert pitch, at quarter note = 92. */
public final class CampfireMelody {
    public static final int BPM = 92;
    public static final int REST = -1;
    // Standard MIDI note numbers: middle C is C4 (60).
    private static final int C3 = 48, D3 = 50, E3 = 52, G3 = 55;
    private static final int A3 = 57, B3 = 59, C4 = 60, D4 = 62;
    private static final int GUITAR_SAMPLE_MIDI = 54; // Vanilla guitar sample: F#3.
    private static final int UNITS_PER_EIGHTH = 600; // 20 ticks/s * 60 s/min / 2.

    // Duration is counted in eighth notes: 1=eighth, 2=quarter, 3=dotted quarter, 4=half.
    private static final Note[] SCORE = {
        // Bar 1: Cmaj7.
        n(C3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 2.
        n(C4, 1), n(B3, 1), n(A3, 1), n(G3, 1), n(A3, 1), n(B3, 1), n(G3, 2),
        // Bar 3: G.
        n(D3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 4.
        n(C4, 1), n(B3, 1), n(A3, 1), n(G3, 1), n(A3, 1), n(B3, 1), n(D4, 1), n(B3, 1),
        // Bar 5: Em7.
        n(E3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 6.
        n(C4, 1), n(B3, 1), n(A3, 1), n(G3, 1), n(A3, 1), n(B3, 1), n(G3, 2),
        // Bar 7: G.
        n(D3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 8.
        n(C4, 1), n(B3, 1), n(A3, 1), n(G3, 1), n(A3, 1), n(B3, 1), n(D4, 1), n(B3, 1),
        // Bar 9: Cmaj7.
        n(C3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 10.
        n(C4, 1), n(B3, 1), n(A3, 1), n(G3, 1), n(A3, 1), n(B3, 1), n(G3, 2),
        // Bar 11: G.
        n(D3, 2), n(G3, 2), n(B3, 3), n(G3, 1),
        // Bar 12: two quarters and the final half note.
        n(C4, 2), n(B3, 2), n(A3, 4)
    };
    private static final int[] START_EIGHTHS = new int[SCORE.length + 1];

    static {
        for (int i = 0; i < SCORE.length; i++) {
            START_EIGHTHS[i + 1] = START_EIGHTHS[i] + SCORE[i].eighths();
        }
    }

    private CampfireMelody() {}

    private static Note n(int midiNote, int eighths) { return new Note(midiNote, eighths); }

    public static int noteCount() { return SCORE.length; }
    public static int totalEighths() { return START_EIGHTHS[SCORE.length]; }

    /** One note's position on the world clock; its onset identifies each new pluck. */
    public static Playback playbackAt(long gameTime) {
        int loopUnits = totalEighths() * UNITS_PER_EIGHTH;
        // Multiply only a small remainder, so even a very old world's clock cannot overflow.
        int phase = (int) (Math.floorMod(gameTime, (long) loopUnits) * BPM % loopUnits);
        int low = 0, high = SCORE.length;
        while (low + 1 < high) {
            int middle = (low + high) / 2;
            if (START_EIGHTHS[middle] * UNITS_PER_EIGHTH <= phase) low = middle;
            else high = middle;
        }
        Note note = SCORE[low];
        int elapsedUnits = phase - START_EIGHTHS[low] * UNITS_PER_EIGHTH;
        int durationUnits = note.eighths() * UNITS_PER_EIGHTH;
        long startTick = gameTime - elapsedUnits / BPM;
        long endTick = gameTime + (durationUnits - elapsedUnits + BPM - 1) / BPM;
        return new Playback(low, note.midiNote(), startTick, endTick, elapsedUnits, durationUnits);
    }

    public static int noteAt(long gameTime) { return playbackAt(gameTime).midiNote(); }

    /** Concert pitch relative to the guitar sample; our score stays inside 0.5–2.0. */
    public static float pitch(int midiNote) {
        return (float) Math.pow(2.0, (midiNote - GUITAR_SAMPLE_MIDI) / 12.0);
    }

    private record Note(int midiNote, int eighths) {
        private Note {
            if ((midiNote != REST && (midiNote < 42 || midiNote > 66)) || eighths <= 0) {
                throw new IllegalArgumentException("Guitar notes need MIDI 42–66 (or REST) and a positive duration");
            }
        }
    }

    public record Playback(int noteIndex, int midiNote, long startTick, long endTick,
                           int elapsedUnits, int durationUnits) {
        public float progress(float partialTick) {
            return Math.max(0, Math.min(1, (elapsedUnits + partialTick * BPM) / durationUnits));
        }
    }
}
