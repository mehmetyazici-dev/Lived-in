package dev.livedin.scene;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.registry.ModContent;
import dev.livedin.world.EncounterPlacement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Explicit prototype placement. Nothing automatically recreates this group on world load. */
public final class CampfireScene {
    private CampfireScene() {}

    public enum Result {
        SUCCESS("Placed a campfire, one musician, and two companions."),
        UNLOADED("The entire 9 by 9 camp area must be loaded."),
        UNSAFE_GROUND("The camp needs flat, solid, dry ground across a 9 by 9 area."),
        OCCUPIED("The camp needs two blocks of empty air above the ground and no existing travelers."),
        SPAWN_FAILED("Camp placement was cancelled; its blocks and NPCs were rolled back.");

        private final String message;
        Result(String message) { this.message = message; }
        public String message() { return message; }
    }

    public static Result place(ServerLevel level, BlockPos center) {
        return place(level, center, false);
    }

    public static Result placeNatural(ServerLevel level, BlockPos center) {
        return place(level, center, true);
    }

    private static boolean smallPlant(BlockState state) {
        return !state.isAir() && FishingScene.clearFooting(state);
    }

    private static Result place(ServerLevel level, BlockPos center, boolean natural) {
        // Validate everything before modifying the world: never overwrite existing builds.
        var ground = new LinkedHashMap<BlockPos, BlockPos>();
        int extent = natural ? 3 : 4;
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                BlockPos feet = center.offset(x, 0, z);
                if (!level.hasChunkAt(feet)) return Result.UNLOADED;
                if (natural) {
                    BlockPos surface = null;
                    for (int dy = 1; dy >= -1; dy--) {
                        BlockPos candidate = feet.above(dy);
                        if (EncounterPlacement.naturalGround(level.getBlockState(candidate.below()))
                                && (level.getBlockState(candidate).isAir() || smallPlant(level.getBlockState(candidate)))) {
                            surface = candidate;
                            break;
                        }
                    }
                    if (surface == null) return Result.UNSAFE_GROUND;
                    ground.put(feet, surface);
                    feet = surface;
                }
                if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                        || !level.getFluidState(feet.below()).isEmpty()) return Result.UNSAFE_GROUND;
                if (natural && !EncounterPlacement.naturalGround(level.getBlockState(feet.below()))) return Result.UNSAFE_GROUND;
                if ((!level.getBlockState(feet).isAir() && !(natural && smallPlant(level.getBlockState(feet))))
                        || !level.getBlockState(feet.above()).isAir()) {
                    return Result.OCCUPIED;
                }
                if (natural) for (int y = 2; y <= 5; y++)
                    if (!level.getBlockState(feet.above(y)).isAir()) return Result.OCCUPIED;
            }
        }
        if (!level.getEntitiesOfClass(AmbientNpc.class, new AABB(center).inflate(6)).isEmpty()) return Result.OCCUPIED;

        List<AmbientNpc> members = new ArrayList<>();
        int[][] offsets = natural ? new int[][] {{0, 2}, {-2, 0}, {2, 0}}
                : new int[][] {{0, 3}, {-3, 0}, {3, 0}};
        for (int i = 0; i < offsets.length; i++) {
            AmbientNpc npc = ModContent.AMBIENT_NPC.get().create(level);
            if (npc == null) return Result.SPAWN_FAILED;
            BlockPos spot = center.offset(offsets[i][0], 0, offsets[i][1]);
            if (natural) spot = ground.get(spot);
            Vec3 location = Vec3.atBottomCenterOf(spot);
            npc.moveTo(location.x, location.y, location.z);
            npc.setActivity(new CampActivity(i == 0 ? NpcRole.MUSICIAN : NpcRole.CAMP_COMPANION, center), spot);
            npc.setYRot(i == 0 ? 180 : i == 1 ? -90 : 90);
            npc.setYHeadRot(npc.getYRot());
            npc.yBodyRot = npc.getYRot();
            if (i == 0) {
                npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModContent.LUTE.get()));
                npc.setDropChance(EquipmentSlot.OFFHAND, 0);
            }
            members.add(npc);
        }

        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        blocks.put(center, Blocks.CAMPFIRE.defaultBlockState());
        var bench = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        blocks.put(center.offset(natural ? -2 : -3, 0, 1), bench);
        blocks.put(center.offset(natural ? 2 : 3, 0, 1), bench);
        blocks.put(center.offset(0, 0, natural ? 3 : 4), bench);
        blocks.put(center.offset(-2, 0, natural ? 2 : 3), Blocks.BARREL.defaultBlockState());
        blocks.put(center.offset(2, 0, natural ? 2 : 3), Blocks.HAY_BLOCK.defaultBlockState());
        if (natural) {
            var fitted = new LinkedHashMap<BlockPos, BlockState>();
            blocks.forEach((pos, state) -> fitted.put(ground.get(pos), state));
            blocks = fitted;
        }

        var originals = new LinkedHashMap<BlockPos, BlockState>();
        for (BlockPos pos : blocks.keySet()) originals.put(pos, level.getBlockState(pos));

        for (var entry : blocks.entrySet()) {
            if (!level.setBlockAndUpdate(entry.getKey(), entry.getValue())) {
                rollback(level, blocks, originals, members);
                return Result.SPAWN_FAILED;
            }
        }
        for (AmbientNpc npc : members) {
            if (!level.addFreshEntity(npc)) {
                rollback(level, blocks, originals, members);
                return Result.SPAWN_FAILED;
            }
        }
        return Result.SUCCESS;
    }

    private static void rollback(ServerLevel level, LinkedHashMap<BlockPos, BlockState> blocks,
            LinkedHashMap<BlockPos, BlockState> originals, List<AmbientNpc> members) {
        members.forEach(AmbientNpc::discard);
        blocks.forEach((pos, state) -> {
            if (level.getBlockState(pos).equals(state)) level.setBlockAndUpdate(pos, originals.get(pos));
        });
    }
}
