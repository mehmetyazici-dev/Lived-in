package dev.livedin.housing;

import dev.livedin.LivedIn;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Starter designs share one placement contract; another design can be registered here later. */
public final class HouseCatalog {
    public record Design(String id, String title, BlockPos bed, BlockPos entrance) {
        public ResourceLocation template(DyeColor color) {
            return ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, "houses/" + id + "/" + color.getName());
        }
        public BlockPos position(BlockPos origin, Rotation rotation, BlockPos local) {
            BlockPos anchor = StructureTemplate.getZeroPositionWithTransform(origin, Mirror.NONE, rotation, 20, 20);
            return StructureTemplate.transform(local, Mirror.NONE, rotation, BlockPos.ZERO).offset(anchor);
        }
        public boolean place(ServerLevel level, BlockPos origin, Rotation rotation, DyeColor color) {
            var template = level.getStructureManager().get(template(color));
            if (template.isEmpty()) return false;
            BlockPos anchor = template.get().getZeroPositionWithTransform(origin, Mirror.NONE, rotation);
            return template.get().placeInWorld(level, anchor, anchor,
                    new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true), level.random, 2);
        }
    }
    public static final Design STARTER = new Design("starter_house", "Starter House", new BlockPos(13, 1, 7), new BlockPos(11, 0, 17));
    public static List<Design> designs() { return List.of(STARTER); }
    public static Design find(String id) { return designs().stream().filter(d -> d.id().equals(id)).findFirst().orElse(STARTER); }
    private HouseCatalog() {}
}
