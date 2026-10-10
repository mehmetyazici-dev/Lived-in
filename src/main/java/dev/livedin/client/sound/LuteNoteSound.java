package dev.livedin.client.sound;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.CampfireMelody;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** A short, moving sound that cannot outlive its musician or performance. */
public final class LuteNoteSound extends AbstractTickableSoundInstance {
    private final AmbientNpc npc;
    private int age;

    public LuteNoteSound(AmbientNpc npc, int note) {
        super(ModContent.LUTE_NOTE.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.npc = npc;
        volume = 0.45F;
        pitch = CampfireMelody.pitch(note);
        updatePosition();
    }

    @Override
    public boolean canPlaySound() {
        return npc.isAlive() && !npc.isRemoved() && !npc.isSilent() && npc.isPerforming();
    }

    @Override
    public void tick() {
        if (!canPlaySound() || ++age >= CampfireMelody.TICKS_PER_NOTE) stop();
        else updatePosition();
    }

    private void updatePosition() {
        x = npc.getX();
        y = npc.getY() + 1;
        z = npc.getZ();
    }
}