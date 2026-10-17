package dev.livedin.housing;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.FishingScene;
import dev.livedin.world.EncounterPlacement;
import dev.livedin.world.NaturalEncounters;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Homes survive entity unloading. Construction is restricted to newly generated, loaded terrain. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class Neighborhoods extends SavedData {
    public static final int SPACING = 700;
    public record Home(String designId, BlockPos origin, Rotation rotation, DyeColor color) {
        public Home(BlockPos origin, Rotation rotation, DyeColor color) { this(HouseCatalog.STARTER.id(), origin, rotation, color); }
        public BlockPos bed() { var design = HouseCatalog.find(designId); return design.position(origin, rotation, design.bed()); }
        public BlockPos entrance() { var design = HouseCatalog.find(designId); return design.position(origin, rotation, design.entrance()); }
    }
    private final Map<UUID, Home> homes = new HashMap<>();
    private final Map<UUID, BlockPos> requests = new HashMap<>();
    private final Set<String> processed = new HashSet<>();
    private final Map<String, BlockPos> centers = new HashMap<>();
    private static final Map<ServerLevel, Map<ChunkPos, Long>> FRESH = new WeakHashMap<>();

    public static Neighborhoods get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(Neighborhoods::new, Neighborhoods::load), "livedin_neighborhoods");
    }
    public static int region(int coordinate) { return Math.floorDiv(coordinate, SPACING); }
    public static BlockPos center(int rx, int rz) { return new BlockPos(rx * SPACING + SPACING / 2, 0, rz * SPACING + SPACING / 2); }
    public static boolean reservedForHomes(ChunkPos chunk) {
        BlockPos c = center(region(chunk.getMiddleBlockX()), region(chunk.getMiddleBlockZ()));
        return chunk.getMaxBlockX() >= c.getX() - 82 && chunk.getMinBlockX() <= c.getX() + 81
                && chunk.getMaxBlockZ() >= c.getZ() - 70 && chunk.getMinBlockZ() <= c.getZ() + 69;
    }
    public int homeCount() { return homes.size(); }
    public Home home(UUID npc) { return homes.get(npc); }
    public void assign(UUID npc, Home home) { homes.putIfAbsent(npc, home); requests.remove(npc); setDirty(); }
    public void request(AmbientNpc npc) {
        if (!npc.getProfile().wantsNaturalHome() || homes.containsKey(npc.getUUID()) || requests.containsKey(npc.getUUID())) return;
        BlockPos anchor = npc.getActivitySpot() == null ? npc.blockPosition() : npc.getActivitySpot();
        requests.put(npc.getUUID(), anchor.immutable()); setDirty();
    }
    public void died(UUID npc) { if (requests.remove(npc) != null) setDirty(); }

    @SubscribeEvent public static void newChunk(ChunkEvent.Load event) {
        if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof LevelChunk chunk && NaturalEncounters.supported(level))
            FRESH.computeIfAbsent(level, l -> new HashMap<>()).put(chunk.getPos(), level.getGameTime());
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) FRESH.remove(level);
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10 != 0) return;
        Map<ChunkPos, Long> fresh = FRESH.get(level);
        if (fresh == null) return;
        fresh.entrySet().removeIf(e -> level.getGameTime() - e.getValue() > 200);
        if (fresh.isEmpty()) return;
        Set<String> checked = new HashSet<>();
        for (ChunkPos cp : fresh.keySet()) {
            int rx = region(cp.getMiddleBlockX()), rz = region(cp.getMiddleBlockZ());
            String key = rx + ":" + rz;
            if (!checked.add(key)) continue;
            if (get(level).tryRegion(level, rx, rz, fresh)) break;
        }
    }

    public boolean tryRegion(ServerLevel level, int rx, int rz, Map<ChunkPos, Long> fresh) {
        String regionKey = rx + ":" + rz;
        if (!centers.containsKey(regionKey)) {
            int checked = 0;
            search: for (int dx : new int[] {0, 24, -24, 48, -48}) for (int dz : new int[] {0, 24, -24, 48, -48}) {
                String key = regionKey + ":center:" + dx + ":" + dz;
                if (processed.contains(key)) continue;
                BlockPos c = center(rx, rz).offset(dx, 0, dz), corner = c.offset(-34, 0, -22);
                if (!ready(level, corner, fresh)) continue;
                processed.add(key); setDirty(); checked++;
                if (safeSite(level, surfaceOrigin(level, corner))) { centers.put(regionKey, c); setDirty(); break search; }
                if (checked >= 4) return true;
            }
            if (!centers.containsKey(regionKey)) return checked > 0;
        }
        BlockPos center = centers.get(regionKey);
        for (int slot = 0; slot < 6; slot++) {
            String key = rx + ":" + rz + ":" + slot;
            if (processed.contains(key)) continue;
            BlockPos corner = center.offset((slot % 3) * 24 - 34, 0, (slot / 3) * 24 - 22);
            if (!ready(level, corner, fresh)) continue;
            // Consume unsafe opportunities too. Returning to old terrain never retries construction.
            processed.add(key); setDirty();
            BlockPos origin = surfaceOrigin(level, corner);
            if (!safeSite(level, origin)) return true;
            UUID waiting = requests.entrySet().stream().filter(e -> region(e.getValue().getX()) == rx && region(e.getValue().getZ()) == rz)
                    .map(Map.Entry::getKey).sorted().findFirst().orElse(null);
            AmbientNpc newcomer = null;
            if (waiting == null) {
                newcomer = ModContent.AMBIENT_NPC.get().create(level);
                if (newcomer == null) return true;
                newcomer.setUUID(UUID.nameUUIDFromBytes((level.getSeed() + ":" + key).getBytes(StandardCharsets.UTF_8)));
                waiting = newcomer.getUUID();
            }
            boolean wantsHome = newcomer == null || newcomer.getProfile().wantsNaturalHome();
            Home home = new Home(origin, Rotation.NONE, DyeColor.WHITE);
            if (newcomer != null) {
                BlockPos at = home.entrance();
                newcomer.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5); newcomer.setActivitySpot(at);
                if (!level.addFreshEntity(newcomer)) return true;
            }
            if (wantsHome) {
                var original = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
                for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -4, 0), origin.offset(19, 6, 19)))
                    original.put(p.immutable(), level.getBlockState(p));
                // Fill shallow slopes up to three blocks; never dig or replace trees/structures.
                for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) {
                    BlockPos p = origin.offset(x, -1, z);
                    while (FishingScene.clearFooting(level.getBlockState(p))) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2); p = p.below();
                    }
                }
                boolean placed;
                try { placed = HouseCatalog.STARTER.place(level, origin, Rotation.NONE, DyeColor.WHITE); }
                catch (RuntimeException error) {
                    com.mojang.logging.LogUtils.getLogger().warn("Lived-In house placement failed at {}", origin, error); placed = false;
                }
                if (!placed) {
                    original.forEach((p, state) -> level.setBlock(p, state, 2));
                    if (newcomer != null) newcomer.discard(); return true;
                }
                assign(waiting, home);
                com.mojang.logging.LogUtils.getLogger().info("Lived-In natural home at {} for {} in neighborhood {}", origin.toShortString(), waiting, regionKey);
            }
            return true;
        }
        return false;
    }
    private static BlockPos surfaceOrigin(ServerLevel level, BlockPos corner) {
        int y = level.getMinBuildHeight();
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++)
            y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, corner.getX() + x, corner.getZ() + z));
        return corner.atY(y);
    }
    private static boolean ready(ServerLevel level, BlockPos corner, Map<ChunkPos, Long> fresh) {
        for (int cx = corner.getX() >> 4; cx <= (corner.getX() + 19) >> 4; cx++)
            for (int cz = corner.getZ() >> 4; cz <= (corner.getZ() + 19) >> 4; cz++) {
                ChunkPos cp = new ChunkPos(cx, cz);
                if (!fresh.containsKey(cp) || level.getChunkSource().getChunkNow(cx, cz) == null || !level.areEntitiesLoaded(cp.toLong())) return false;
            }
        return true;
    }

    public static boolean safeSite(ServerLevel level, BlockPos origin) {
        int minimum = origin.getY() - 4;
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) {
            BlockPos p = origin.offset(x, 0, z);
            var chunk = level.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4);
            if (chunk == null || chunk.getAllReferences().entrySet().stream().anyMatch(ref -> !ref.getValue().isEmpty()
                    && ref.getKey().step() == net.minecraft.world.level.levelgen.GenerationStep.Decoration.SURFACE_STRUCTURES)
                    || chunk.getAllStarts().values().stream().anyMatch(start -> start.isValid()
                            && start.getBoundingBox().maxY() >= origin.getY() - 4)) return false;
            int ground = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX() & 15, p.getZ() & 15);
            if (ground >= origin.getY() || ground < minimum || !EncounterPlacement.naturalGround(level.getBlockState(p.atY(ground)))) return false;
            for (int dy = ground + 1; dy <= origin.getY() + 6; dy++)
                if (!clearance(level.getBlockState(p.atY(dy)))) return false;
        }
        return level.getEntities(null, new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(origin),
                net.minecraft.world.phys.Vec3.atLowerCornerOf(origin.offset(20, 7, 20)))).isEmpty();
    }
    private static boolean clearance(net.minecraft.world.level.block.state.BlockState state) {
        return FishingScene.clearFooting(state) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN);
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag h = new ListTag(), r = new ListTag(), p = new ListTag();
        homes.forEach((id, home) -> {
            CompoundTag t = new CompoundTag(); t.putUUID("Npc", id); t.putLong("Origin", home.origin.asLong());
            t.putString("Design", home.designId); t.putString("Rotation", home.rotation.name()); t.putInt("Color", home.color.getId()); h.add(t);
        });
        requests.forEach((id, pos) -> { CompoundTag t = new CompoundTag(); t.putUUID("Npc", id); t.putLong("Anchor", pos.asLong()); r.add(t); });
        processed.forEach(k -> p.add(net.minecraft.nbt.StringTag.valueOf(k)));
        ListTag c = new ListTag(); centers.forEach((key, pos) -> {
            CompoundTag t = new CompoundTag(); t.putString("Region", key); t.putLong("Position", pos.asLong()); c.add(t);
        });
        tag.put("Centers", c); tag.put("Homes", h); tag.put("Requests", r); tag.put("Processed", p); return tag;
    }
    public static Neighborhoods load(CompoundTag tag, HolderLookup.Provider registries) {
        Neighborhoods data = new Neighborhoods();
        for (var v : tag.getList("Centers", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)v; data.centers.put(t.getString("Region"), BlockPos.of(t.getLong("Position")));
        }
        for (var v : tag.getList("Homes", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)v;
            data.homes.put(t.getUUID("Npc"), new Home(t.contains("Design") ? t.getString("Design") : HouseCatalog.STARTER.id(),
                    BlockPos.of(t.getLong("Origin")), Rotation.valueOf(t.getString("Rotation")), DyeColor.byId(t.getInt("Color"))));
        }
        for (var v : tag.getList("Requests", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)v; data.requests.put(t.getUUID("Npc"), BlockPos.of(t.getLong("Anchor")));
        }
        for (var v : tag.getList("Processed", Tag.TAG_STRING)) data.processed.add(v.getAsString());
        return data;
    }
}
