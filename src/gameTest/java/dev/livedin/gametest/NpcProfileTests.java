package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.entity.activity.TravelerActivity;
import dev.livedin.entity.profile.NpcEligibility;
import dev.livedin.entity.profile.NpcProfile;
import dev.livedin.entity.profile.NpcStats;
import dev.livedin.registry.ModContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NpcProfileTests {
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static AmbientNpc npc(GameTestHelper h) {
        floor(h);
        return h.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(12, 1, 12));
    }

    private static CompoundTag save(GameTestHelper h, AmbientNpc npc) {
        CompoundTag tag = new CompoundTag();
        h.assertTrue(npc.save(tag), "NPC saves through the vanilla serializer");
        return tag;
    }

    private static AmbientNpc load(GameTestHelper h, CompoundTag tag) {
        AmbientNpc npc = (AmbientNpc) EntityType.loadEntityRecursive(tag, h.getLevel(), e -> e);
        h.assertTrue(npc != null, "Registry reconstructs saved NPC");
        return npc;
    }

    @GameTest(template = "arena")
    public static void profileSurvivesRealReloadAndTravel(GameTestHelper h) {
        AmbientNpc original = npc(h);
        NpcProfile profile = original.getProfile();
        UUID id = original.getUUID();
        original.setHealth(7);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        CompoundTag saved = save(h, original);
        original.discard();
        AmbientNpc restored = load(h, saved);
        h.getLevel().addFreshEntity(restored);
        h.runAfterDelay(3, () -> {
            h.assertTrue(id.equals(restored.getUUID()) && profile.equals(restored.getProfile()), "Person and all rolled values survive loading");
            h.assertTrue(restored.getHealth() == 7 && restored.getMainHandItem().is(Items.STICK), "Loading never heals or replaces equipment");
            h.assertTrue(profile.name().equals(restored.getCustomName().getString()), "Vanilla name is visible to clients");
            restored.setActivity(new TravelerActivity(), restored.blockPosition().offset(1400, 0, 0));
            h.assertTrue(profile.equals(restored.getProfile()), "Moving across future neighborhoods cannot reroll a person");
            h.assertTrue(saved.getCompound(NpcProfile.SAVE_KEY).equals(save(h, restored).getCompound(NpcProfile.SAVE_KEY)),
                    "Saving again does not change profile or home request");
            h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "One original NPC, no replacements");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void rolesHaveIndependentPersistentPermissions(GameTestHelper h) {
        floor(h);
        for (NpcRole role : NpcRole.values()) {
            CompoundTag tag = save(h, npc(h));
            tag.remove(NpcProfile.SAVE_KEY);
            tag.putInt("NpcRole", role.id());
            AmbientNpc person = load(h, tag);
            var profile = person.getProfile();
            NpcEligibility e = profile.eligibility();
            boolean camp = role == NpcRole.MUSICIAN || role == NpcRole.CAMP_COMPANION;
            h.assertTrue(e.naturalHome() == !camp && e.recruitment() == !camp && e.barter() == !camp
                    && e.nightlyCamp() == !camp && e.defense() == (role != NpcRole.MUSICIAN), "Explicit role eligibility: " + role);
            h.assertTrue(!camp || !profile.wantsNaturalHome(), "Camp members never request a home");
            person.setActivity(new TravelerActivity(), person.blockPosition());
            h.assertTrue(profile.equals(person.getProfile()), "Activity change cannot rewrite personal permissions");
            AmbientNpc reloaded = load(h, save(h, person));
            h.assertTrue(e.equals(reloaded.getProfile().eligibility()), "Saved permissions survive a different activity role");
        }
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void campAssignmentBeforeFirstTickUsesCampPolicy(GameTestHelper h) {
        AmbientNpc npc = npc(h);
        npc.setActivity(new CampActivity(NpcRole.CAMP_COMPANION, npc.blockPosition().offset(0, 0, -3)), npc.blockPosition());
        h.runAfterDelay(2, () -> {
            h.assertTrue(!npc.getProfile().eligibility().recruitment() && npc.getProfile().eligibility().defense(),
                    "Initialization uses placed scene role, not constructor's default traveler role");
            h.assertTrue(!npc.getProfile().wantsNaturalHome(), "Camp companion excluded from home roll");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void legacyFisherMigrationPreservesIdentityAndAssociations(GameTestHelper h) {
        AmbientNpc original = npc(h);
        original.setCustomName(Component.literal("Old Fisher"));
        original.setHealth(9);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        CompoundTag legacy = save(h, original);
        legacy.remove(NpcProfile.SAVE_KEY);
        UUID cat = UUID.randomUUID();
        legacy.putInt("NpcRole", 3); legacy.putUUID("PetCat", cat);
        legacy.putLong("PetSpot", original.blockPosition().offset(1, 0, 0).asLong());
        legacy.putLong("FishingWater", original.blockPosition().offset(0, -1, 4).asLong());
        AmbientNpc migrated = load(h, legacy);
        NpcProfile profile = migrated.getProfile();
        CompoundTag saved = save(h, migrated);
        h.assertTrue(migrated.getUUID().equals(original.getUUID()) && migrated.getHealth() == 9, "Migration retains identity and damage");
        h.assertTrue(profile.name().equals("Old Fisher") && migrated.getMainHandItem().is(Items.FISHING_ROD), "Existing name and rod retained");
        for (String key : new String[] {"PetCat", "PetSpot", "FishingWater"})
            h.assertTrue(legacy.get(key).equals(saved.get(key)), "Original association retained: " + key);
        h.assertTrue(profile.equals(load(h, saved).getProfile()), "Migration happens once, then reads saved fields");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void boatPassengerProfileRoundTrip(GameTestHelper h) {
        AmbientNpc rider = npc(h);
        Boat boat = h.spawn(EntityType.BOAT, new BlockPos(12, 1, 12));
        h.assertTrue(rider.startRiding(boat, true), "Original rider mounts boat");
        NpcProfile profile = rider.getProfile();
        UUID riderId = rider.getUUID(), boatId = boat.getUUID();
        CompoundTag saved = new CompoundTag();
        h.assertTrue(boat.save(saved), "Boat saves passenger tree");
        rider.discard(); boat.discard();
        Boat restored = (Boat) EntityType.loadEntityRecursive(saved, h.getLevel(), e -> e);
        h.getLevel().addFreshEntityWithPassengers(restored);
        h.assertTrue(boatId.equals(restored.getUUID()) && restored.getPassengers().size() == 1, "Original boat and one passenger");
        AmbientNpc passenger = (AmbientNpc) restored.getFirstPassenger();
        h.assertTrue(riderId.equals(passenger.getUUID()) && profile.equals(passenger.getProfile()) && passenger.getVehicle() == restored,
                "Passenger profile and reciprocal mount identity survive");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void rollsStayInRangeAndHomeDecisionIsBalanced(GameTestHelper h) {
        AmbientNpc npc = ModContent.AMBIENT_NPC.get().create(h.getLevel());
        int homes = 0, lowHealth = 0, highHealth = 0;
        for (int i = 0; i < 1000; i++) {
            npc.setUUID(new UUID(0x51a7edL, i));
            NpcProfile p = NpcProfile.create(npc);
            NpcStats s = p.stats();
            h.assertTrue(s.health() >= 16 && s.health() <= 30 && s.strength() >= 1 && s.strength() <= 4
                    && s.walkingPace() >= 90 && s.walkingPace() <= 110 && s.marksmanship() >= 0 && s.marksmanship() <= 100
                    && s.craftsmanship() >= 0 && s.craftsmanship() <= 100, "Roll respects accepted ranges");
            h.assertTrue(p.equals(NpcProfile.create(npc)), "Same identity has stable migration fallback");
            homes += p.wantsNaturalHome() ? 1 : 0;
            lowHealth += s.health() < 20 ? 1 : 0; highHealth += s.health() > 20 ? 1 : 0;
        }
        h.assertTrue(homes > 440 && homes < 560 && lowHealth > 0 && highHealth > 0,
                "Seeded sample includes both home outcomes near 50% and varied health: " + homes);
        h.assertTrue(NpcStats.DEFAULT.equals(NpcStats.load(new CompoundTag())), "Missing stat fields use accepted defaults");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void savedOverridesAndPartialDataDoNotReroll(GameTestHelper h) {
        AmbientNpc npc = npc(h);
        CompoundTag saved = save(h, npc);
        CompoundTag profile = saved.getCompound(NpcProfile.SAVE_KEY);
        profile.putString("Name", "Mira");
        profile.put("Eligibility", new NpcEligibility(false, false, true, false, true).save());
        profile.putBoolean("WantsNaturalHome", true);
        CompoundTag stats = new CompoundTag();
        stats.putInt("Health", 30); stats.putInt("Strength", 4); stats.putInt("WalkingPace", 110);
        stats.putInt("Craftsmanship", 900);
        profile.put("Stats", stats);
        AmbientNpc loaded = load(h, saved);
        var p = loaded.getProfile();
        h.assertTrue(!p.eligibility().recruitment() && p.eligibility().barter(), "Future type can barter while opting out of recruitment");
        h.assertTrue(!p.wantsNaturalHome(), "Ineligible NPC never gains a house from contradictory data");
        h.assertTrue(p.stats().marksmanship() == 50 && p.stats().craftsmanship() == 100, "Missing field defaults; out-of-range score clamps");
        h.assertTrue(loaded.getMaxHealth() == 30 && loaded.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4
                && Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) - Math.sqrt(0.11)) < 0.00001,
                "Saved stats applied to real health, melee, and movement attributes");
        h.assertTrue(p.equals(load(h, save(h, loaded)).getProfile()), "Saved overrides remain stable");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void walkingPaceMatchesPlayerReference(GameTestHelper h) {
        floor(h);
        AmbientNpc walker = new AmbientNpc(ModContent.AMBIENT_NPC.get(), h.getLevel()) {
            @Override protected void registerGoals() { }
        };
        Vec3 start = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(12, 1, 3)));
        walker.moveTo(start.x, start.y, start.z);
        h.getLevel().addFreshEntity(walker);
        walker.getProfile();
        walker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(NpcStats.DEFAULT.movementSpeed());
        double[] warmedZ = {0};
        for (int tick = 1; tick < 45; tick++) h.runAtTickTime(tick, () ->
                walker.getMoveControl().setWantedPosition(start.x, start.y, start.z + 17, 1.0));
        h.runAtTickTime(20, () -> warmedZ[0] = walker.getZ());
        h.runAtTickTime(40, () -> {
            double distance = walker.getZ() - warmedZ[0];
            h.assertTrue(distance > 4.2 && distance < 4.45, "100% pace at goal speed 1 travels about 4.317 blocks/sec: " + distance);
            h.succeed();
        });
    }
}
