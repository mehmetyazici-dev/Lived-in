package dev.livedin.scene;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Manual shoreline placement; validates terrain without digging or changing blocks. */
public final class FishingScene {
    private FishingScene() {}
    public enum Result {
        SUCCESS("Placed one fisher and their sitting cat."),
        UNSAFE("Needs a dry 3 by 3 bank and exposed 3 by 3 source water, 3 to 5 blocks away and one block lower."),
        OCCUPIED("This shoreline already contains a traveler or cat."),
        SPAWN_FAILED("Fishing scene placement was cancelled; its new entities were removed.");
        private final String message;
        Result(String message) { this.message = message; }
        public String message() { return message; }
    }

    public static boolean safeShore(Level level, BlockPos feet) {
        return level.hasChunkAt(feet) && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getFluidState(feet.below()).isEmpty() && !level.getBlockState(feet.below()).is(Blocks.MAGMA_BLOCK);
    }

    public static boolean validFishingSpot(Level level, BlockPos shore, BlockPos water) {
        long dx = (long)water.getX() - shore.getX(), dz = (long)water.getZ() - shore.getZ();
        if (water.getY() != shore.getY() - 1 || dx * dx + dz * dz < 9 || dx * dx + dz * dz > 25) return false;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (!safeShore(level, shore.offset(x, 0, z))) return false;
            BlockPos surface = water.offset(x, 0, z);
            if (!level.hasChunkAt(surface) || !level.getBlockState(surface).is(Blocks.WATER)
                    || !level.getFluidState(surface).is(FluidTags.WATER) || !level.getFluidState(surface).isSource()
                    || !level.getBlockState(surface.above()).isAir() || !level.getBlockState(surface.above(2)).isAir()) return false;
        }
        // Require the casting corridor to be loaded before ray testing; never request remote chunks.
        for (int x = Math.min(shore.getX(), water.getX()) - 1; x <= Math.max(shore.getX(), water.getX()) + 1; x++)
            for (int z = Math.min(shore.getZ(), water.getZ()) - 1; z <= Math.max(shore.getZ(), water.getZ()) + 1; z++)
                if (!level.hasChunkAt(new BlockPos(x, shore.getY(), z))) return false;
        Vec3 start = Vec3.atBottomCenterOf(shore).add(0, 1.5, 0);
        Vec3 end = Vec3.atCenterOf(water).add(0, 0.55, 0);
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }

    public static Result place(ServerLevel level, BlockPos shore, BlockPos water) {
        if (!validFishingSpot(level, shore, water)) return Result.UNSAFE;
        AABB area = new AABB(shore).inflate(3);
        if (!level.getEntitiesOfClass(AmbientNpc.class, area).isEmpty() || !level.getEntitiesOfClass(Cat.class, area).isEmpty())
            return Result.OCCUPIED;
        AmbientNpc fisher = ModContent.AMBIENT_NPC.get().create(level);
        Cat cat = EntityType.CAT.create(level);
        if (fisher == null || cat == null) return Result.SPAWN_FAILED;
        Direction facing = Direction.getNearest(water.getX() - shore.getX(), 0, water.getZ() - shore.getZ());
        BlockPos catSpot = shore.relative(facing.getClockWise());
        Vec3 at = Vec3.atBottomCenterOf(shore), petAt = Vec3.atBottomCenterOf(catSpot);
        fisher.moveTo(at.x, at.y, at.z, facing.toYRot(), 0);
        fisher.yBodyRot = fisher.getYRot();
        fisher.setYHeadRot(fisher.getYRot());
        fisher.assignFishingScene(shore, water, cat.getUUID(), catSpot);
        fisher.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        fisher.setDropChance(EquipmentSlot.MAINHAND, 0);
        cat.moveTo(petAt.x, petAt.y, petAt.z, facing.toYRot(), 0);
        cat.setTame(true, true);
        // Vanilla only resolves player owners. This saved UUID is our association, not NPC-follow AI.
        cat.setOwnerUUID(fisher.getUUID());
        cat.setOrderedToSit(true);
        cat.setInSittingPose(true);
        cat.setPersistenceRequired();
        cat.addTag("livedin_fishing_cat");
        if (!level.addFreshEntity(cat) || !level.addFreshEntity(fisher)) {
            cat.discard(); fisher.discard(); return Result.SPAWN_FAILED;
        }
        return Result.SUCCESS;
    }
}
