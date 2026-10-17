package dev.livedin.entity;

import dev.livedin.entity.activity.NpcActivities;
import dev.livedin.entity.activity.NpcActivity;
import dev.livedin.entity.goal.ActivityGoal;
import dev.livedin.entity.goal.ActivityLookGoal;
import dev.livedin.entity.goal.ReturnToActivitySpotGoal;
import dev.livedin.entity.goal.SceneStrollGoal;
import dev.livedin.entity.profile.NpcProfile;
import dev.livedin.entity.profile.NpcStats;
import dev.livedin.entity.life.NpcRoutine;
import dev.livedin.entity.life.NpcDefense;
import dev.livedin.entity.goal.RoutineGoal;
import dev.livedin.housing.Neighborhoods;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/** Shared traveler and Minecraft hooks. The current activity owns its scene state and rules. */
public class AmbientNpc extends PathfinderMob implements net.minecraft.world.entity.monster.RangedAttackMob {
    public static final int ACTIVITY_RADIUS = 6;
    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    static { NpcActivities.initializeSynchedData(); }
    @Nullable private BlockPos activitySpot;
    @Nullable private NpcActivity activity;
    @Nullable private NpcProfile profile;
    private final NpcRoutine routine = new NpcRoutine();
    private final NpcDefense defense = new NpcDefense();
    private final dev.livedin.social.NpcRelationships relationships = new dev.livedin.social.NpcRelationships();
    private int viewerProgress;
    private boolean activityPaused;

    public AmbientNpc(EntityType<? extends AmbientNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) ground.setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, NpcStats.DEFAULT.movementSpeed()).add(Attributes.FOLLOW_RANGE, 16.0);
    }
    /** Component-owned keys must be allocated for this entity before its first data builder. */
    public static <T> EntityDataAccessor<T> activityDataKey(EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(AmbientNpc.class, serializer);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, NpcRole.TRAVELER.id());
        NpcActivities.defineSynchedData(builder);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new dev.livedin.entity.goal.DefendGoal(this));
        goalSelector.addGoal(2, new RoutineGoal(this));
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(4, new ActivityGoal(this));
        goalSelector.addGoal(5, new ReturnToActivitySpotGoal(this, 1.0));
        goalSelector.addGoal(6, new SceneStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new ActivityLookGoal(this));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }
    @Override public void tick() {
        if (!level().isClientSide) getProfile();
        // Capture placement only after Minecraft has positioned the new entity.
        if (!level().isClientSide && activitySpot == null) setActivitySpot(blockPosition());
        if (level() instanceof net.minecraft.server.level.ServerLevel server && !isNoAi() && isAlive()) {
            if (tickCount % 100 == 0 && dev.livedin.world.NaturalEncounters.supported(server)) Neighborhoods.get(server).request(this);
            routine.prepare(this, level().getDayTime());
            defense.tick(this);
            if (!allowsDayActivity() && !activityPaused) { getActivity().onPaused(this); activityPaused = true; }
            else if (allowsDayActivity() && activityPaused) { getActivity().onResumed(this); activityPaused = false; }
        }
        super.tick();
        if (!level().isClientSide && allowsDayActivity()) getActivity().tick(this);
    }
    public NpcRoutine getRoutine() { return routine; }
    public NpcDefense getDefense() { return defense; }
    public dev.livedin.social.NpcRelationships getRelationships() { return relationships; }
    public int getViewerProgress() { return viewerProgress; }
    public void setViewerProgress(int value) { viewerProgress = Math.clamp(value, 0, 20); }
    @Override protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) return net.minecraft.world.InteractionResult.PASS;
        if (level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server) || !isAlive() || player.isSpectator())
            return net.minecraft.world.InteractionResult.PASS;
        if (relationships.progress(player.getUUID()) >= 20 && !player.isShiftKeyDown() && getProfile().eligibility().recruitment()) {
            dev.livedin.social.ConversationMenu.open(this, server); return net.minecraft.world.InteractionResult.CONSUME;
        }
        return relationships.offer(this, server, hand) ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
    }
    public boolean allowsDayActivity() { return !routine.active() && getTarget() == null; }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)
            defense.attacked(this, attacker);
        return hurt;
    }
    @Override public void performRangedAttack(net.minecraft.world.entity.LivingEntity target, float power) {
        if (!defense.valid(this, target)) return;
        var arrow = net.minecraft.world.entity.projectile.ProjectileUtil.getMobArrow(this,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), power, getMainHandItem());
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        arrow.shoot(dx, target.getY(.333) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * .2, dz, 1.6F, 6);
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, 1, 1);
        level().addFreshEntity(arrow);
    }
    @Override protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) { }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (isSleeping()) stopSleeping();
            routine.cleanupFire(this); Neighborhoods.get(server).died(getUUID());
            dev.livedin.social.Invitations.get(server).died(getUUID());
        }
        super.die(source);
    }
    /** Full profiles stay on the server; vanilla synchronizes names and visible attributes. */
    public NpcProfile getProfile() {
        if (level().isClientSide) throw new IllegalStateException("Profiles are owned by the server");
        if (profile == null) {
            profile = NpcProfile.create(this);
            profile.apply(this, true);
        }
        return profile;
    }
    public NpcRole getRole() { return NpcRole.fromId(entityData.get(ROLE)); }
    public NpcActivity getActivity() {
        // Client activities are views of synchronized state; they never run server decisions.
        if (activity == null || (level().isClientSide && activity.role() != getRole()))
            activity = NpcActivities.create(getRole());
        return activity;
    }
    public void setActivity(NpcActivity next, BlockPos spot) {
        if (level().isClientSide) throw new IllegalStateException("Activities are assigned by the server");
        replaceActivity(Objects.requireNonNull(next));
        setActivitySpot(spot);
    }
    private void replaceActivity(NpcActivity next) {
        getActivity().onStopped(this);
        getNavigation().stop();
        activity = next;
        entityData.set(ROLE, next.role().id());
        next.onAssigned(this);
    }
    @Nullable public BlockPos getActivitySpot() { return activitySpot; }
    public void setActivitySpot(BlockPos pos) {
        activitySpot = pos.immutable();
        restrictTo(activitySpot, getActivity().radius());
        getNavigation().stop();
    }
    public boolean isWithinActivityArea(BlockPos pos) {
        int radius = getActivity().radius();
        return activitySpot != null && activitySpot.distSqr(pos) < radius * radius
                && getActivity().isSafePosition(this, pos);
    }
    public boolean isSafeActivityPath(Path path) {
        for (int i = 0; i < path.getNodeCount(); i++)
            if (!getActivity().isSafePosition(this, path.getNode(i).asBlockPos())) return false;
        return true;
    }
    @Override public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        NpcProfile savedProfile = level().isClientSide ? profile : getProfile();
        super.addAdditionalSaveData(tag);
        if (savedProfile != null) tag.put(NpcProfile.SAVE_KEY, savedProfile.save());
        tag.put("LivedInRoutine", routine.save()); tag.put("LivedInDefense", defense.save(this));
        tag.put("LivedInRelationships", relationships.save());
        if (activitySpot != null) tag.putLong("ActivitySpot", activitySpot.asLong());
        tag.putInt("NpcRole", getRole().id());
        getActivity().save(tag);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        replaceActivity(NpcActivities.load(tag));
        if (!level().isClientSide) {
            profile = tag.contains(NpcProfile.SAVE_KEY, Tag.TAG_COMPOUND)
                    ? NpcProfile.load(tag.getCompound(NpcProfile.SAVE_KEY), this) : NpcProfile.create(this);
            profile.apply(this, false);
        }
        activitySpot = null;
        if (tag.contains("ActivitySpot", Tag.TAG_LONG)) setActivitySpot(BlockPos.of(tag.getLong("ActivitySpot")));
        routine.load(tag.getCompound("LivedInRoutine")); defense.load(tag.getCompound("LivedInDefense"), this);
        relationships.load(tag.getCompound("LivedInRelationships"));
        if (routine.active()) clearRestriction();
        // Vanilla restores equipment and identity; activities do not recreate removed items or scene members.
        setPersistenceRequired();
    }
}
