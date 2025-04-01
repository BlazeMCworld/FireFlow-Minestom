package de.blazemcworld.fireflow.command;

import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.entity.Player;

public class LocateCommand extends Command {

    public LocateCommand() {
        super("locate", "find");

        addSyntax((sender, ctx) -> {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(ctx.get("player"));
            if (player == null) {
                sender.sendMessage(Component.text(Translations.get("error.locate.not_found")).color(NamedTextColor.RED));
                return;
            }

            Space space = SpaceManager.getSpaceForPlayer(player);
            if (space == null) {
                sender.sendMessage(Component.text(Translations.get("success.locate.at_spawn", player.getUsername())).color(NamedTextColor.AQUA));
                return;
            }

            if (space.play == player.getInstance()) {
                sender.sendMessage(Component.text(Translations.get("success.locate.playing_on", player.getUsername(), space.info.name, String.valueOf(space.info.id))).color(NamedTextColor.AQUA));
                return;
            }

            if (space.code == player.getInstance()) {
                sender.sendMessage(Component.text(Translations.get("success.locate.coding_on", player.getUsername(), space.info.name, String.valueOf(space.info.id))).color(NamedTextColor.AQUA));
                return;
            }

            if (space.build == player.getInstance()) {
                sender.sendMessage(Component.text(Translations.get("success.locate.building_on", player.getUsername(), space.info.name, String.valueOf(space.info.id))).color(NamedTextColor.AQUA));
                return;
            }

            sender.sendMessage(Component.text(Translations.get("error.internal")).color(NamedTextColor.RED));
        }, new ArgumentString("player"));
    }

}
