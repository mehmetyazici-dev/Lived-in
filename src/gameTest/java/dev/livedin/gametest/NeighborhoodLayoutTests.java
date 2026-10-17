package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.housing.Neighborhoods;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NeighborhoodLayoutTests {
    @GameTest(template="housing_arena", batch="housing_layout", timeoutTicks=150)
    public static void sixUnloadedHomeRequestsBecomeAdjacentPersistentHouses(GameTestHelper h) {
        for(int x=0;x<72;x++) for(int z=0;z<48;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.GRASS_BLOCK);
            for(int y=1;y<=13;y++) h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        BlockPos origin=h.absolutePos(new BlockPos(2,1,2));
        int rx=Neighborhoods.region(origin.getX()),rz=Neighborhoods.region(origin.getZ());
        var tag=new CompoundTag(); var c=new CompoundTag(); var centers=new ListTag(); var requests=new ListTag();
        c.putString("Region",rx+":"+rz); c.putLong("Position",origin.offset(34,0,22).atY(0).asLong()); centers.add(c);tag.put("Centers",centers);
        UUID[] ids=new UUID[6];
        for(int i=0;i<6;i++) {
            ids[i]=UUID.randomUUID();var r=new CompoundTag();r.putUUID("Npc",ids[i]);r.putLong("Anchor",origin.asLong());requests.add(r);
        }
        tag.put("Requests",requests);var data=Neighborhoods.load(tag,h.getLevel().registryAccess());
        var fresh=new HashMap<ChunkPos,Long>();
        for(int x=origin.getX()>>4;x<=(origin.getX()+67)>>4;x++) for(int z=origin.getZ()>>4;z<=(origin.getZ()+43)>>4;z++) fresh.put(new ChunkPos(x,z),h.getLevel().getGameTime());
        h.runAfterDelay(10,()-> {
            for(int i=0;i<6;i++) h.assertTrue(data.tryRegion(h.getLevel(),rx,rz,fresh),"One ready home request handled per placement");
            h.assertTrue(data.homeCount()==6,"Six requested homes placed in one neighborhood");
            var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());var loaded=Neighborhoods.load(saved,h.getLevel().registryAccess());
            var origins=new java.util.HashSet<BlockPos>();
            for(UUID id:ids) {
                var home=loaded.home(id);origins.add(home.origin());
                h.assertTrue(h.getLevel().getBlockState(home.bed()).is(Blocks.WHITE_BED),"Assigned original person's house and bed exist after record reload");
            }
            h.assertTrue(origins.size()==6 && origins.stream().allMatch(p->p.getX()>=origin.getX() && p.getX()<=origin.getX()+48
                    && p.getZ()>=origin.getZ() && p.getZ()<=origin.getZ()+24),"Distinct adjacent plots form a compact 3 by 2 neighborhood");
            h.assertTrue(!loaded.tryRegion(h.getLevel(),rx,rz,fresh),"Reloaded processing state prevents duplicate houses");h.succeed();
        });
    }
}
