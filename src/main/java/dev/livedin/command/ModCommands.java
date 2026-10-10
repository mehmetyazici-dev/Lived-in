package dev.livedin.command;

import dev.livedin.LivedIn;
import dev.livedin.scene.CampfireScene;
import dev.livedin.scene.FishingScene;
import dev.livedin.scene.BoatScene;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("livedin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("camp")
                        .executes(context -> place(context.getSource(),
                                BlockPos.containing(context.getSource().getPosition()).relative(
                                        context.getSource().getEntity() == null
                                                ? net.minecraft.core.Direction.SOUTH
                                                : context.getSource().getEntity().getDirection(), 5)))
                        .then(Commands.argument("center", BlockPosArgument.blockPos())
                                .executes(context -> place(context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "center")))))
                .then(Commands.literal("fishing")
                        .then(Commands.argument("shore", BlockPosArgument.blockPos())
                                .then(Commands.argument("water", BlockPosArgument.blockPos())
                                        .executes(context -> placeFishing(context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "shore"),
                                                BlockPosArgument.getLoadedBlockPos(context, "water"))))))
                .then(Commands.literal("boat")
                        .then(Commands.argument("start", BlockPosArgument.blockPos())
                                .then(Commands.argument("end", BlockPosArgument.blockPos())
                                        .executes(context -> placeBoat(context.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(context, "start"),
                                                BlockPosArgument.getLoadedBlockPos(context, "end")))))));
    }

    private static int place(CommandSourceStack source, BlockPos center) {
        var result = CampfireScene.place(source.getLevel(), center);
        if (result != CampfireScene.Result.SUCCESS) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int placeBoat(CommandSourceStack source, BlockPos start, BlockPos end) {
        var result = BoatScene.place(source.getLevel(), start, end);
        if (result != BoatScene.Result.SUCCESS) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int placeFishing(CommandSourceStack source, BlockPos shore, BlockPos water) {
        var result = FishingScene.place(source.getLevel(), shore, water);
        if (result != FishingScene.Result.SUCCESS) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }
}
