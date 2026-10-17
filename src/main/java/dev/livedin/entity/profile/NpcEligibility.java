package dev.livedin.entity.profile;

import dev.livedin.entity.NpcRole;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Independent permissions, initially chosen from the scene role and then saved with the person. */
public record NpcEligibility(boolean naturalHome, boolean recruitment, boolean barter,
        boolean nightlyCamp, boolean defense) {
    public static NpcEligibility forRole(NpcRole role) {
        return switch (role) {
            case MUSICIAN -> new NpcEligibility(false, false, false, false, false);
            case CAMP_COMPANION -> new NpcEligibility(false, false, false, false, true);
            default -> new NpcEligibility(true, true, true, true, true);
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("NaturalHome", naturalHome); tag.putBoolean("Recruitment", recruitment);
        tag.putBoolean("Barter", barter); tag.putBoolean("NightlyCamp", nightlyCamp); tag.putBoolean("Defense", defense);
        return tag;
    }

    public static NpcEligibility load(CompoundTag tag, NpcEligibility fallback) {
        return new NpcEligibility(flag(tag, "NaturalHome", fallback.naturalHome),
                flag(tag, "Recruitment", fallback.recruitment), flag(tag, "Barter", fallback.barter),
                flag(tag, "NightlyCamp", fallback.nightlyCamp), flag(tag, "Defense", fallback.defense));
    }

    private static boolean flag(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }
}
