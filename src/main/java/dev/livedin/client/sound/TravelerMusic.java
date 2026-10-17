package dev.livedin.client.sound;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.CampActivity;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import dev.livedin.scene.CampfireMelody;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Exactly one voice per loaded musician; discarded immediately on unload or world change. */
public final class TravelerMusic {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("livedin.musicDiagnostics");
    private static final boolean NOTE_DIAGNOSTICS = Boolean.getBoolean("livedin.noteDiagnostics");
    private static final Map<UUID, Voice> VOICES = new HashMap<>();
    private static ClientLevel lastLevel;

    private TravelerMusic() {}

    public static void tick(Minecraft game) {
        if (game.level != lastLevel) {
            VOICES.values().forEach(voice -> voice.stop(game));
            if (DIAGNOSTICS && !VOICES.isEmpty()) LOGGER.info("Lived-In music: cleared {} voices on world change", VOICES.size());
            VOICES.clear();
            lastLevel = game.level;
        }
        if (game.level == null || game.player == null || game.isPaused()) return;
        var heard = new HashSet<UUID>();
        for (var entity : game.level.entitiesForRendering()) {
            if (!(entity instanceof AmbientNpc npc) || !npc.isAlive() || !CampActivity.isPerforming(npc) || npc.isSilent()
                    || npc.distanceToSqr(game.player) > 24 * 24) continue;
            UUID id = npc.getUUID();
            heard.add(id);
            Voice voice = VOICES.get(id);
            if (voice == null || voice.npc != npc) {
                if (voice != null) voice.stop(game);
                voice = new Voice(npc);
                VOICES.put(id, voice);
                if (DIAGNOSTICS) LOGGER.info("Lived-In music: voice opened for {}; total {} (one note per voice)", id, VOICES.size());
            }
            voice.tick(game);
        }
        var iterator = VOICES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!heard.contains(entry.getKey())) {
                entry.getValue().stop(game);
                iterator.remove();
                if (DIAGNOSTICS) LOGGER.info("Lived-In music: voice closed for {}; total {}", entry.getKey(), VOICES.size());
            }
        }
    }

    private static final class Voice {
        private final AmbientNpc npc;
        private long lastOnset = Long.MIN_VALUE;
        private LuteNoteSound sound;
        private boolean playbackConfirmed;

        private Voice(AmbientNpc npc) { this.npc = npc; }

        private void tick(Minecraft game) {
            if (DIAGNOSTICS && !playbackConfirmed && sound != null && game.getSoundManager().isActive(sound)) {
                playbackConfirmed = true;
                LOGGER.info("Lived-In music: sound engine playback active for {}", npc.getUUID());
            }
            long time = game.level.getGameTime();
            var playback = CampfireMelody.playbackAt(time);
            if (playback.startTick() == lastOnset) return;
            lastOnset = playback.startTick();
            stop(game);
            int note = playback.midiNote();
            if (note >= 0) {
                sound = new LuteNoteSound(npc, note, playback.endTick());
                game.getSoundManager().play(sound);
                if (NOTE_DIAGNOSTICS) LOGGER.info("Lived-In score: musician {}; index {}; MIDI {}; onset {}; end {}; pitch {}",
                        npc.getUUID(), playback.noteIndex(), note, playback.startTick(), playback.endTick(), CampfireMelody.pitch(note));
            }
        }

        private void stop(Minecraft game) {
            if (sound != null) {
                game.getSoundManager().stop(sound);
                sound = null;
            }
        }
    }
}
