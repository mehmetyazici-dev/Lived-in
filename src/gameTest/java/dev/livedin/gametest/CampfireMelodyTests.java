package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.scene.CampfireMelody;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CampfireMelodyTests {
    @GameTest(template = "arena")
    public static void preservesScoreLengthAndConcertPitch(GameTestHelper h) {
        h.assertTrue(CampfireMelody.noteCount() == 64 && CampfireMelody.totalEighths() == 96,
                "The supplied twelve 4/4 bars contain 64 plucks over 48 quarter beats");
        h.assertTrue(CampfireMelody.noteAt(0) == 48, "A-string fret 3 is concert C3, not written C4");
        h.assertTrue(CampfireMelody.noteAt(105) == 50, "Bar 3 starts on A-string fret 5: D3");
        h.assertTrue(CampfireMelody.noteAt(209) == 52, "Bar 5 starts on A-string fret 7: E3");
        h.assertTrue(Math.abs(CampfireMelody.pitch(54) - 1) < 0.00001F, "Guitar F#3 sample is unshifted");
        h.assertTrue(Math.abs(CampfireMelody.pitch(48) - 0.70710678F) < 0.00001F, "C3 retains its actual pitch");
        for (int tick = 0; tick < 627; tick++) {
            float pitch = CampfireMelody.pitch(CampfireMelody.noteAt(tick));
            h.assertTrue(pitch >= 0.5F && pitch <= 2, "Every score note fits the sound engine pitch range");
        }
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void playsDottedEighthAndHalfNoteRhythms(GameTestHelper h) {
        int[] ticks = {0, 14, 27, 46, 53};
        int[] notes = {48, 55, 59, 55, 60};
        for (int i = 0; i < ticks.length; i++) {
            var note = CampfireMelody.playbackAt(ticks[i]);
            h.assertTrue(note.midiNote() == notes[i] && note.startTick() == ticks[i],
                    "Opening rhythm must be quarter, quarter, dotted quarter, eighth");
            h.assertTrue(note.progress(0) >= 0 && note.progress(0) < 1, "Picking pose follows current note");
        }
        h.assertTrue(CampfireMelody.playbackAt(45).midiNote() == 59, "Dotted B3 lasts until tick 46");
        var ending = CampfireMelody.playbackAt(600);
        h.assertTrue(ending.noteIndex() == 63 && ending.midiNote() == 57
                        && ending.startTick() == 600 && ending.endTick() == 627,
                "Final A3 is a half note, not two repeated quarter-note attacks");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void maintains92BpmWithoutLoopDrift(GameTestHelper h) {
        // Independent musical oracle: 48 beats * 60 seconds * 20 ticks / 92 BPM.
        for (long loop = 1; loop <= 200; loop++) {
            long expected = (loop * 48 * 60 * 20 + 91) / 92;
            var first = CampfireMelody.playbackAt(expected);
            h.assertTrue(first.noteIndex() == 0 && first.startTick() == expected,
                    "Each loop starts within one tick of the absolute 92 BPM score time");
            h.assertTrue(CampfireMelody.playbackAt(expected - 1).noteIndex() == 63,
                    "Previous loop's final note lasts up to its correct boundary");
        }
        var oldWorld = CampfireMelody.playbackAt(1_000_000_000_000L);
        h.assertTrue(oldWorld.startTick() <= 1_000_000_000_000L && oldWorld.endTick() > 1_000_000_000_000L,
                "A very old world still resolves a valid note interval");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void joinsCurrentNoteAndDistinguishesNewPlucks(GameTestHelper h) {
        var joining = CampfireMelody.playbackAt(30);
        h.assertTrue(joining.midiNote() == 59 && joining.startTick() == 27 && joining.endTick() == 46,
                "Arriving mid-note uses its original end instead of extending the beat");
        h.assertTrue(CampfireMelody.playbackAt(40).startTick() == joining.startTick(),
                "Repeated client ticks identify one current pluck");
        var firstG = CampfireMelody.playbackAt(14);
        var secondG = CampfireMelody.playbackAt(46);
        h.assertTrue(firstG.midiNote() == secondG.midiNote() && firstG.startTick() != secondG.startTick(),
                "The same pitch later in the score is a separate attack");
        h.assertTrue(CampfireMelody.playbackAt(-1).noteIndex() == 63 && CampfireMelody.playbackAt(0).noteIndex() == 0,
                "Clock wrap resolves the last and first notes safely");
        h.succeed();
    }
}
