package org.agmas.noellesroles.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import org.agmas.noellesroles.voice.HeliumBuzzCommand;

/**
 * NoellesRoles 所有命令的统一注册入口。
 * 新增调试 / 管理命令时，在 {@link #registerAll(CommandDispatcher)} 里追加一行即可，
 * 不用每个命令类自己挂 {@code CommandRegistrationCallback}。
 */
public final class NoellesRolesCommands {

    private NoellesRolesCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> registerAll(dispatcher));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        HeliumBuzzCommand.register(dispatcher);
    }
}
