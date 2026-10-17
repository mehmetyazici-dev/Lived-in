package dev.livedin.gametest;

import dev.livedin.LivedIn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HouseTemplateTests {
    @GameTest(template = "arena")
    public static void importedHousePlacesInEveryColorAndRotation(GameTestHelper h) {
        verify(h, "houses/starter_house", DyeColor.WHITE);
        for (DyeColor color : DyeColor.values()) verify(h, "houses/starter_house/" + color.getName(), color);
        h.succeed();
    }

    private static void verify(GameTestHelper h, String templatePath, DyeColor color) {
        var template = h.getLevel().getStructureManager()
                .get(ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, templatePath)).orElseThrow();
        var coloredBed = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_bed"));
        var coloredCarpet = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName() + "_carpet"));
        h.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(20, 6, 20)), "Imported footprint and height");
        BlockPos base = h.absolutePos(new BlockPos(2, 1, 2));
        for (Rotation rotation : Rotation.values()) {
            for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
            var settings = new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true);
            BlockPos origin = template.getZeroPositionWithTransform(base, Mirror.NONE, rotation);
            var box = template.getBoundingBox(settings, origin);
            h.assertTrue(box.minX() == base.getX() && box.maxX() == base.getX() + 19
                    && box.minZ() == base.getZ() && box.maxZ() == base.getZ() + 19, "Rotated house stays in plot");
            h.assertTrue(template.placeInWorld(h.getLevel(), origin, origin, settings, h.getLevel().random, 2), "Template places");
            int beds = 0, accentCarpets = 0, blackCarpets = 0, doors = 0, chests = 0, sponges = 0;
            for (BlockPos pos : BlockPos.betweenClosed(base, base.offset(19, 5, 19))) {
                var state = h.getLevel().getBlockState(pos);
                if (state.is(coloredBed)) {
                    beds++;
                    h.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == rotation.rotate(Direction.EAST), "Bed facing rotates");
                    h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BedBlockEntity,
                            "Both halves retain bed block entities");
                }
                if (state.is(coloredCarpet)) accentCarpets++;
                if (state.is(Blocks.BLACK_CARPET)) blackCarpets++;
                if (state.is(Blocks.ACACIA_DOOR)) {
                    doors++;
                    h.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == rotation.rotate(Direction.NORTH), "Door facing rotates");
                }
                if (state.is(Blocks.CHEST)) {
                    chests++;
                    h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest && chest.isEmpty(), "Chests load empty");
                }
                if (state.is(Blocks.SPONGE)) sponges++;
            }
            h.assertTrue(beds == 4 && accentCarpets == (color == DyeColor.BLACK ? 20 : 14)
                    && blackCarpets == (color == DyeColor.BLACK ? 20 : 6) && doors == 4 && chests == 6 && sponges == 0,
                    "Original furnishings, doors, and accents survive " + color + " / " + rotation);
        }
    }
}
