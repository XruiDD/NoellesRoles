package org.agmas.noellesroles.voice;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 调试命令：手动施加/清除 HELIUM_BUZZ。
 * <p>
 * {@code /heliumbuzz <player> <seconds> [amplifier]} —— 施加（seconds=0 等价于清除）
 */
public final class HeliumBuzzCommand {

    private HeliumBuzzCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("noellesroles:heliumbuzz")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                        .executes(ctx -> run(ctx, 0))
                                        .then(CommandManager.argument("amplifier", IntegerArgumentType.integer(0, 10))
                                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "amplifier"))))))
        );
    }

    private static int run(CommandContext<ServerCommandSource> ctx, int amplifier) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        HeliumBuzzPlayerComponent comp = HeliumBuzzPlayerComponent.get(target);

        if (seconds <= 0) {
            comp.clear();
            ctx.getSource().sendFeedback(
                    () -> Text.literal("Cleared helium_buzz from " + target.getName().getString()), true);
        } else {
            comp.apply(seconds * 20, amplifier);
            ctx.getSource().sendFeedback(
                    () -> Text.literal("Applied helium_buzz to " + target.getName().getString()
                            + " for " + seconds + "s (amplifier " + amplifier + ")"), true);
        }
        return 1;
    }
}
