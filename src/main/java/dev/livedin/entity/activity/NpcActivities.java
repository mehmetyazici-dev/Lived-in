package dev.livedin.entity.activity;

import dev.livedin.entity.NpcRole;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;

/** Construction and synchronization definitions for activities; no scene decisions live here. */
public final class NpcActivities {
    private static final Map<NpcRole, Supplier<NpcActivity>> FACTORIES = Map.of(
            NpcRole.TRAVELER, TravelerActivity::new,
            NpcRole.MUSICIAN, () -> new CampActivity(NpcRole.MUSICIAN, null),
            NpcRole.CAMP_COMPANION, () -> new CampActivity(NpcRole.CAMP_COMPANION, null),
            NpcRole.FISHER, FishingActivity::new,
            NpcRole.BOAT_RIDER, BoatActivity::new);

    private NpcActivities() {}

    public static void initializeSynchedData() {
        CampActivity.initializeSynchedData();
        FishingActivity.initializeSynchedData();
    }

    public static NpcActivity create(NpcRole role) { return FACTORIES.get(role).get(); }

    public static NpcActivity load(CompoundTag tag) {
        NpcActivity activity = create(NpcRole.fromId(tag.getInt("NpcRole")));
        activity.load(tag);
        return activity;
    }

    public static void defineSynchedData(SynchedEntityData.Builder builder) {
        // Keep the previous order: ROLE, PERFORMING, FISHING_WATER, FISHING_TICK.
        CampActivity.defineSynchedData(builder);
        FishingActivity.defineSynchedData(builder);
    }
}
