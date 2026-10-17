package dev.livedin.scene;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.FishingActivity;
import dev.livedin.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
        UNSAFE("Needs safe dry footing directly beside exposed source water, room for the cat, and a clear cast 3 to 5 blocks into the water."),
        OCCUPIED("This shoreline already contains a traveler or cat."),
        SPAWN_FAILED("Fishing scene placement was cancelled; its new entities were removed.");
        private final String message;
        Result(String message) { this.message = message; }
        public String message() { return message; }
    }

    public static boolean safeShore(Level level, BlockPos feet) {
        return level.hasChunkAt(feet) && clearFooting(level.getBlockState(feet)) && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getFluidState(feet.below()).isEmpty() && !level.getBlockState(feet.below()).is(Blocks.MAGMA_BLOCK);
    }

    public static boolean clearFooting(net.minecraft.world.level.block.state.BlockState state) {
        return state.isAir() || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.DEAD_BUSH) || (state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)
                && !state.is(Blocks.WITHER_ROSE));
    }

    public static BlockPos findPetSpot(Level level, BlockPos shore, BlockPos water) {
        Direction face = Direction.getNearest(water.getX() - shore.getX(), 0, water.getZ() - shore.getZ());
        for (Direction side : new Direction[] {face.getClockWise(), face.getCounterClockWise(), face.getOpposite()})
            for (int dy : new int[] {0, 1, -1}) {
                BlockPos spot = shore.relative(side).above(dy);
                if (safeShore(level, spot)) return spot;
            }
        return null;
    }

    public static boolean clearCast(Level level, Vec3 start, BlockPos water) {
        if (!level.hasChunkAt(water) || !level.getBlockState(water).is(Blocks.WATER)
                || !level.getFluidState(water).isSource() || !level.getBlockState(water.above()).isAir()
                || !level.getBlockState(water.above(2)).isAir()) return false;
        for (int x = Math.min((int)Math.floor(start.x), water.getX()) - 1; x <= Math.max((int)Math.floor(start.x), water.getX()) + 1; x++)
            for (int z = Math.min((int)Math.floor(start.z), water.getZ()) - 1; z <= Math.max((int)Math.floor(start.z), water.getZ()) + 1; z++)
                if (!level.hasChunkAt(new BlockPos(x, water.getY(), z))) return false;
        return level.clip(new ClipContext(start, Vec3.atCenterOf(water).add(0, 0.55, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    /** Casting distance is separate from bank distance: water must touch the fisher's front block. */
    public static boolean waterfront(Level level, BlockPos shore, BlockPos target) {
        if ((long)shore.getY() - target.getY() != 1) return false;
        Direction face = Direction.getNearest(target.getX() - (double)shore.getX(), 0,
                target.getZ() - (double)shore.getZ());
        BlockPos edge = shore.relative(face).below();
        return level.hasChunkAt(edge) && level.getBlockState(edge).is(Blocks.WATER)
                && level.getFluidState(edge).isSource() && level.getBlockState(edge.above()).isAir()
                && level.getBlockState(edge.above(2)).isAir();
    }

    public static boolean validFishingSpot(Level level, BlockPos shore, BlockPos water) {
        long dx = (long)water.getX() - shore.getX(), dz = (long)water.getZ() - shore.getZ();
        if (dx * dx + dz * dz < 9 || dx * dx + dz * dz > 25
                || !safeShore(level, shore) || !waterfront(level, shore, water)
                || findPetSpot(level, shore, water) == null) return false;
        // Require the casting corridor to be loaded before ray testing; never request remote chunks.
        for (int x = Math.min(shore.getX(), water.getX()) - 1; x <= Math.max(shore.getX(), water.getX()) + 1; x++)
            for (int z = Math.min(shore.getZ(), water.getZ()) - 1; z <= Math.max(shore.getZ(), water.getZ()) + 1; z++)
                if (!level.hasChunkAt(new BlockPos(x, shore.getY(), z))) return false;
        return clearCast(level, Vec3.atBottomCenterOf(shore).add(0, 1.5, 0), water);
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
        BlockPos catSpot = findPetSpot(level, shore, water);
        Vec3 at = Vec3.atBottomCenterOf(shore), petAt = Vec3.atBottomCenterOf(catSpot);
        fisher.moveTo(at.x, at.y, at.z, facing.toYRot(), 0);
        fisher.yBodyRot = fisher.getYRot();
        fisher.setYHeadRot(fisher.getYRot());
        fisher.setActivity(new FishingActivity(water, cat.getUUID(), catSpot), shore);
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
