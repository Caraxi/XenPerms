package au.com.craftau.xenperms.commands;

import au.com.craftau.xenperms.XenPerms;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import java.util.Locale;

public class RankCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> rankCommand = Commands
                .literal("rank")
                .then(
                    Commands.argument("player", MessageArgument.message())
                            .requires((commandSource -> commandSource.hasPermissionLevel(4)))
                            .executes(RankCommand::executeOtherPlayer)
                )
                .executes(RankCommand::execute);
        dispatcher.register(rankCommand);
    }
    public static int execute(CommandContext<CommandSource> c) {
        try {
            XenPerms.LOGGER.info(c.getSource().getName() + " used '/rank'");
            ServerPlayerEntity player = c.getSource().asPlayer();
            XenPerms.Instance.UpdatePlayerRank(player);
        } catch (Exception ex) {
            XenPerms.LOGGER.error(ex.getMessage());
            return 0;
        }
        return 1;
    }

    public static void messageSenderOrServer(CommandSource source, String message) {
        try {

            ServerPlayerEntity player = source.asPlayer();
            XenPerms.Instance.SendPlayerMessage(player, message);
        } catch (Exception ex) {
            XenPerms.LOGGER.info(message);
        }
    }

    public static int executeOtherPlayer(CommandContext<CommandSource> c) {
        try {
            ITextComponent messageValue = MessageArgument.getMessage(c, "player");
            String name = messageValue.getString().toLowerCase(Locale.ROOT);
            XenPerms.LOGGER.info(c.getSource().getName() + " used '/rank " + name + "'");
            for (PlayerEntity player :  c.getSource().getServer().getPlayerList().getPlayers()) {
                if (player.getName().getString().toLowerCase(Locale.ROOT).equals(name)) {
                    XenPerms.Instance.UpdatePlayerRank(player);
                    messageSenderOrServer(c.getSource(), "Updated rank of '" + player.getName().getString() + "'.");
                    return 1;
                }
            }
            messageSenderOrServer(c.getSource(), "Could not find player with name '" + name + "'.");

        } catch (Exception ex) {
            XenPerms.LOGGER.error(ex.getMessage());
            return 0;
        }
        return 1;
    }
}
