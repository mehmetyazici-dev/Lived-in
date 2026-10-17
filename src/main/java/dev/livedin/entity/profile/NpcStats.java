package dev.livedin.entity.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Saved personal attributes. The two skill scores have no gameplay effects yet. */
public record NpcStats(int health, int strength, int walkingPace, int marksmanship, int craftsmanship) {
    public static final NpcStats DEFAULT = new NpcStats(20, 1, 100, 50, 50);

    public NpcStats {
        health = Mth.clamp(health, 16, 30);
        strength = Mth.clamp(strength, 1, 4);
        walkingPace = Mth.clamp(walkingPace, 90, 110);
        marksmanship = Mth.clamp(marksmanship, 0, 100);
        craftsmanship = Mth.clamp(craftsmanship, 0, 100);
    }

    public static NpcStats roll(RandomSource random) {
        return new NpcStats(near(random, 16, 30, 20), near(random, 1, 4, 1),
                near(random, 90, 110, 100), near(random, 0, 100, 50), near(random, 0, 100, 50));
    }

    // A triangular distribution makes ordinary values more common than extreme values.
    private static int near(RandomSource random, int min, int max, int mode) {
        double u = random.nextDouble();
        double split = (double)(mode - min) / (max - min);
        return (int)Math.round(u < split ? min + Math.sqrt(u * (max - min) * (mode - min))
                : max - Math.sqrt((1 - u) * (max - min) * (max - mode)));
    }

    /** Mob movement applies speed both to acceleration and forward input; pace scales its square. */
    public double movementSpeed() { return Math.sqrt(0.1 * walkingPace / 100.0); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Health", health); tag.putInt("Strength", strength);
        tag.putInt("WalkingPace", walkingPace); tag.putInt("Marksmanship", marksmanship);
        tag.putInt("Craftsmanship", craftsmanship);
        return tag;
    }

    public static NpcStats load(CompoundTag tag) {
        return new NpcStats(number(tag, "Health", DEFAULT.health), number(tag, "Strength", DEFAULT.strength),
                number(tag, "WalkingPace", DEFAULT.walkingPace), number(tag, "Marksmanship", DEFAULT.marksmanship),
                number(tag, "Craftsmanship", DEFAULT.craftsmanship));
    }

    private static int number(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }
}
