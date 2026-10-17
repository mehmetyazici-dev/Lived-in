package dev.livedin.entity.profile;

import dev.livedin.entity.AmbientNpc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Server-owned identity, permissions and home decision; activity components never own these values. */
public record NpcProfile(String name, String background, String personality, NpcStats stats,
        NpcEligibility eligibility, boolean wantsNaturalHome) {
    public static final String SAVE_KEY = "LivedInProfile";
    private static final String[] NAMES = {"Rowan", "Ash", "Hazel", "Finch", "Willow", "Reed", "Sage", "Robin",
            "Alder", "Juniper", "Briar", "Fern", "Jasper", "Mira", "Ellis", "Wren"};
    private static final String[] BACKGROUNDS = {"I used to live near a spruce forest.",
            "I grew up beside a river.", "I've traveled between quiet farming villages.",
            "My old home was close to the mountains.", "I spent my childhood near the coast."};
    private static final String[] PERSONALITIES = {"I prefer quiet mornings.", "I enjoy company around a fire.",
            "I like finding new paths.", "I always appreciate a warm meal.", "I feel at home outdoors."};

    public NpcProfile {
        wantsNaturalHome = wantsNaturalHome && eligibility.naturalHome();
    }

    public static NpcProfile create(AmbientNpc npc) {
        // Stable per-person seed also makes migration reproducible before its first save.
        var id = npc.getUUID();
        RandomSource random = RandomSource.create(id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 23));
        String name = NAMES[random.nextInt(NAMES.length)];
        String background = BACKGROUNDS[random.nextInt(BACKGROUNDS.length)];
        String personality = PERSONALITIES[random.nextInt(PERSONALITIES.length)];
        NpcStats stats = NpcStats.roll(random);
        NpcEligibility eligibility = NpcEligibility.forRole(npc.getRole());
        boolean homeRoll = random.nextBoolean();
        if (npc.hasCustomName()) name = npc.getCustomName().getString();
        return new NpcProfile(name, background, personality, stats, eligibility, homeRoll);
    }

    public static NpcProfile load(CompoundTag tag, AmbientNpc npc) {
        NpcProfile fallback = create(npc);
        return new NpcProfile(text(tag, "Name", fallback.name), text(tag, "Background", fallback.background),
                text(tag, "Personality", fallback.personality), NpcStats.load(tag.getCompound("Stats")),
                NpcEligibility.load(tag.getCompound("Eligibility"), fallback.eligibility),
                tag.contains("WantsNaturalHome", Tag.TAG_ANY_NUMERIC) ? tag.getBoolean("WantsNaturalHome") : fallback.wantsNaturalHome);
    }

    private static String text(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) && !tag.getString(key).isBlank() ? tag.getString(key) : fallback;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1); tag.putString("Name", name);
        tag.putString("Background", background); tag.putString("Personality", personality);
        tag.put("Stats", stats.save()); tag.put("Eligibility", eligibility.save());
        tag.putBoolean("WantsNaturalHome", wantsNaturalHome);
        return tag;
    }

    public String homeStatus() {
        return !eligibility.naturalHome() ? "excluded" : wantsNaturalHome ? "awaiting a home" : "no natural home";
    }

    /** Reapply bases once after load; do not heal a damaged or previously saved NPC. */
    public void apply(AmbientNpc npc, boolean fresh) {
        float health = npc.getHealth();
        boolean fullHealth = health >= npc.getMaxHealth();
        npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.health());
        npc.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stats.strength());
        npc.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(stats.movementSpeed());
        npc.setHealth(fresh && fullHealth ? npc.getMaxHealth() : Math.min(health, npc.getMaxHealth()));
        if (!npc.hasCustomName()) npc.setCustomName(Component.literal(name));
    }
}
