package dev.livedin.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import dev.livedin.LivedIn;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

/** One opportunity per new candidate chunk. Reloading never repairs or replenishes an encounter. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class NaturalEncounters {
    public static final int REGION_CHUNKS = 8;
    public static final int PENDING = 1;
    public static final int PROCESSED = 2;
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LivedIn.MOD_ID);
    public static final Supplier<AttachmentType<Integer>> STATE = ATTACHMENTS.register("encounter_state",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());
    private static final Map<ServerLevel, LinkedHashSet<ChunkPos>> QUEUES = new WeakHashMap<>();

    private NaturalEncounters() {}
    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }

    public static ChunkPos candidate(long seed, int regionX, int regionZ) {
        RandomSource random = RandomSource.create(seed ^ (regionX * 341873128712L)
                ^ (regionZ * 132897987541L) ^ 0x4C49564544494EL);
        return new ChunkPos(regionX * REGION_CHUNKS + 2 + random.nextInt(4),
                regionZ * REGION_CHUNKS + 2 + random.nextInt(4));
    }

    /** Four land opportunities per old eight-chunk region; boats retain their original spacing. */
    public static ChunkPos landCandidate(long seed, int cellX, int cellZ) {
        RandomSource random = RandomSource.create(seed ^ (cellX * 341873128712L)
                ^ (cellZ * 132897987541L) ^ 0x53484F52454C414EL);
        return new ChunkPos(cellX * 4 + 1 + random.nextInt(2), cellZ * 4 + 1 + random.nextInt(2));
    }

    public static boolean isBoatCandidate(long seed, ChunkPos pos) {
        return pos.equals(candidate(seed, Math.floorDiv(pos.x, REGION_CHUNKS), Math.floorDiv(pos.z, REGION_CHUNKS)));
    }

    public static boolean isLandCandidate(long seed, ChunkPos pos) {
        return pos.equals(landCandidate(seed, Math.floorDiv(pos.x, 4), Math.floorDiv(pos.z, 4)));
    }

    public static boolean isCandidate(long seed, ChunkPos pos) {
        return isLandCandidate(seed, pos) || isBoatCandidate(seed, pos);
    }

    /** Keep one tenth of the previous quarter-rate camps, without reducing fishers or boats. */
    public static boolean allowsCamp(long seed, ChunkPos pos) {
        RandomSource random = RandomSource.create(seed ^ (pos.x * 341873128712L)
                ^ (pos.z * 132897987541L) ^ 0x43414D5052415445L);
        return random.nextInt(4) == 0 && random.nextInt(10) == 0;
    }

    public static boolean supported(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD
                && !(level.getChunkSource().getGenerator() instanceof FlatLevelSource);
    }

    @SubscribeEvent
    public static void onLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)
                || !supported(level)) return;
        // Do not query the world inside this callback: the chunk may not be FULL yet.
        if (event.isNewChunk() && !chunk.hasData(STATE.get()) && isCandidate(level.getSeed(), chunk.getPos())) {
            chunk.setData(STATE.get(), PENDING);
        }
        if (chunk.hasData(STATE.get()) && chunk.getData(STATE.get()) == PENDING) {
            QUEUES.computeIfAbsent(level, key -> new LinkedHashSet<>()).add(chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var queue = QUEUES.get(level);
        if (queue == null || queue.isEmpty()) return;
        // Rotate waiting entries, inspect at most eight, and scan at most one ready chunk per tick.
        int checks = Math.min(8, queue.size());
        for (int i = 0; i < checks; i++) {
            ChunkPos pos = queue.iterator().next();
            queue.remove(pos);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null || !level.areEntitiesLoaded(pos.toLong())) {
                queue.add(pos);
                continue;
            }
            if (dev.livedin.housing.Neighborhoods.reservedForHomes(pos)) chunk.setData(STATE.get(), PROCESSED);
            else process(level, chunk);
            break;
        }
    }

    /** Also used by checks; only a persisted pending candidate can be consumed. */
    public static boolean process(ServerLevel level, LevelChunk chunk) {
        return process(level, chunk, allowsCamp(level.getSeed(), chunk.getPos()));
    }

    /** Explicit camp permission also allows controlled placement fixtures in development checks. */
    public static boolean process(ServerLevel level, LevelChunk chunk, boolean allowCamp) {
        if (!chunk.hasData(STATE.get()) || chunk.getData(STATE.get()) != PENDING) return false;
        chunk.setData(STATE.get(), PROCESSED);
        // Skip all chunks referenced by a generated structure, including its neighboring pieces.
        if (chunk.getAllReferences().values().stream().anyMatch(refs -> !refs.isEmpty())
                || chunk.getAllStarts().values().stream().anyMatch(start -> start.isValid())) return false;
        var placed = EncounterPlacement.tryPlace(level, chunk, isBoatCandidate(level.getSeed(), chunk.getPos()), allowCamp);
        if (placed != null) LogUtils.getLogger().info("Lived-In natural {} at {} (seed {}, chunk {})",
                placed.kind(), placed.position().toShortString(), level.getSeed(), chunk.getPos());
        return placed != null;
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && QUEUES.containsKey(level))
            QUEUES.get(level).remove(event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) QUEUES.remove(level);
    }
}
