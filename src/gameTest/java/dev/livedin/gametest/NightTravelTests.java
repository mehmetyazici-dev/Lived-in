package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.BoatActivity;
import dev.livedin.housing.HouseCatalog;
import dev.livedin.housing.Neighborhoods;
import dev.livedin.scene.BoatScene;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NightTravelTests {
    @AfterBatch(batch = "house_night") public static void resetHouseTime(ServerLevel level) { level.setDayTime(6000); }
    @AfterBatch(batch = "boat_night") public static void resetBoatTime(ServerLevel level) { level.setDayTime(6000); }
    private static void floor(GameTestHelper h) {
        for (int x=0;x<24;x++) for(int z=0;z<24;z++) h.setBlock(new BlockPos(x,0,z), Blocks.GRASS_BLOCK);
    }
    @GameTest(template = "arena", batch = "house_night", timeoutTicks = 700)
    public static void walksThroughDoorSleepsAndReturnsAtDawn(GameTestHelper h) {
        floor(h); BlockPos origin = h.absolutePos(new BlockPos(2,1,2));
        HouseCatalog.STARTER.place(h.getLevel(), origin, Rotation.NONE, DyeColor.WHITE);
        var npc = h.spawn(dev.livedin.registry.ModContent.AMBIENT_NPC.get(),new BlockPos(13,1,20));
        npc.setActivitySpot(npc.blockPosition()); var anchor = npc.blockPosition();
        Neighborhoods.get(h.getLevel()).assign(npc.getUUID(),new Neighborhoods.Home(origin,Rotation.NONE,DyeColor.WHITE));
        h.setNight();
        h.runAtTickTime(350,()-> {
            h.assertTrue(npc.isSleeping(),"NPC navigates from outdoors through the double doors into its bed; pos="+npc.blockPosition());
            h.setDayTime(0);
        });
        h.runAtTickTime(650,()-> {
            h.assertTrue(!npc.isSleeping() && npc.allowsDayActivity() && npc.blockPosition().distSqr(anchor)<36,
                    "Dawn wakes the same NPC and returns it to its original activity"); h.succeed();
        });
    }
    @GameTest(template = "arena", batch = "boat_night", timeoutTicks = 1150)
    public static void docksOriginalBoatThenRemountsAfterDawn(GameTestHelper h) {
        floor(h);
        for(int x=5;x<=18;x++) for(int z=3;z<=19;z++) h.setBlock(new BlockPos(x,0,z),Blocks.WATER);
        var start=h.absolutePos(new BlockPos(10,0,7));
        h.assertTrue(BoatScene.place(h.getLevel(),start,start.offset(0,0,8))==BoatScene.Result.SUCCESS,"Boat scene places");
        var npc=h.getLevel().getEntitiesOfClass(AmbientNpc.class,h.getBounds()).getFirst();
        // A boat rider's explicit shore-camp schedule also applies if it has a natural-home claim.
        Neighborhoods.get(h.getLevel()).assign(npc.getUUID(),new Neighborhoods.Home(h.absolutePos(new BlockPos(2,1,2)),Rotation.NONE,DyeColor.WHITE));
        var boat=(Boat)npc.getVehicle(); UUID id=boat.getUUID(); h.setNight();
        h.runAtTickTime(500,()-> {
            h.assertTrue(!npc.isPassenger() && npc.getActivity() instanceof BoatActivity,"Night routine docks without changing boat role; pos="+npc.blockPosition());
            h.assertTrue(npc.getDefense().armed(npc),"Docked rider carries its assigned weapon");
            var saved = npc.getRoutine().save();
            h.assertTrue(saved.contains("Fire"), "Docked rider actually places a campfire");
            var fire = BlockPos.of(saved.getLong("Fire"));
            h.assertTrue(h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE) && npc.blockPosition().distSqr(fire) <= 36,
                    "Rider stays within six blocks of its campfire");
            for(int x=0;x<24;x++) for(int z=0;z<24;z++) {
                var pushed=h.absolutePos(new BlockPos(x,1,z));
                if (pushed.distSqr(fire)>36 && pushed.distSqr(npc.blockPosition())<64
                        && dev.livedin.scene.FishingScene.safeShore(h.getLevel(),pushed)) {
                    npc.teleportTo(pushed.getX()+.5,pushed.getY(),pushed.getZ()+.5);
                    x=24; break;
                }
            }
            h.runAtTickTime(750, () -> {
                h.assertTrue(npc.blockPosition().distSqr(fire)<=36,"Displaced rider walks back near the campfire");
                h.setDayTime(0);
                h.runAfterDelay(5, () -> h.assertTrue(!h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE), "Dawn removes boat campfire"));
            });
        });
        h.runAtTickTime(1100,()-> {
            h.assertTrue(npc.getVehicle()==boat && id.equals(boat.getUUID()) && npc.getMainHandItem().is(Items.FISHING_ROD)
                    && npc.allowsDayActivity(),"Dawn restores rod and original mount, then resumes fishing/travel");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class,h.getBounds()).size()==1,"No replacement boat"); h.succeed();
        });
    }
}
