package dev.livedin.client.sound;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.CampfireMelody;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** A short, moving sound that cannot outlive its musician or performance. */
public final class LuteNoteSound extends AbstractTickableSoundInstance {
    private final AmbientNpc npc;
    private final long endTick;

    public LuteNoteSound(AmbientNpc npc, int note, long endTick) {
        super(ModContent.LUTE_NOTE.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.npc = npc;
        this.endTick = endTick;
        volume = 0.45F;
        pitch = CampfireMelody.pitch(note);
        updatePosition();
    }

    @Override
    public boolean canPlaySound() {
        return npc.isAlive() && !npc.isRemoved() && !npc.isSilent() && CampActivity.isPerforming(npc);
    }

    @Override
    public void tick() {
        if (!canPlaySound() || npc.level().getGameTime() >= endTick) stop();
        else updatePosition();
    }

    private void updatePosition() {
        x = npc.getX();
        y = npc.getY() + 1;
        z = npc.getZ();
    }
}
