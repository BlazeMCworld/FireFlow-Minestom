package de.blazemcworld.fireflow.command;

import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Transfer;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;

public class ClearCommand extends Command {

    public ClearCommand() {
        super("clear");

        addSyntax((sender, ctx) -> {
            Space space = getSpace(sender);
            if (space == null) return;

            space.reload("clear_world");
            sender.sendMessage(Component.text(Translations.get("success.cleared.world")).color(NamedTextColor.GREEN));
        }, new ArgumentLiteral("world"));

        addSyntax((sender, ctx) -> {
            Space space = getSpace(sender);
            if (space == null) return;

            for (Player p : space.code.getPlayers()) {
                space.editor.stopAction(p);
            }

            space.editor.rootWidgets.clear();
            for (Entity e : space.code.getEntities()) {
                if (e instanceof Player || e.getEntityMeta() instanceof InteractionMeta) continue;
                e.remove();
            }
            space.editor.functions.clear();
            space.editor.lockedWidgets.clear();

            space.reload("clear_code");
            sender.sendMessage(Component.text(Translations.get("success.cleared.code")).color(NamedTextColor.GREEN));
        }, new ArgumentLiteral("code"));

        addSyntax((sender, ctx) -> {
            Space space = getSpace(sender);
            if (space == null) return;

            space.info.builders.clear();
            sender.sendMessage(Component.text(Translations.get("success.cleared.builders")).color(NamedTextColor.GREEN));

            for (Player each : space.code.getPlayers()) {
                if (each == sender) continue;
                Transfer.move(each, space.play);
            }
        }, new ArgumentLiteral("builders"));

        addSyntax((sender, ctx) -> {
            Space space = getSpace(sender);
            if (space == null) return;

            space.info.developers.clear();
            sender.sendMessage(Component.text(Translations.get("success.cleared.developers")).color(NamedTextColor.GREEN));

            for (Player each : space.code.getPlayers()) {
                if (each == sender) continue;
                Transfer.move(each, space.play);
            }
        }, new ArgumentLiteral("developers"));

        addSyntax((sender, ctx) -> {
            Space space = getSpace(sender);
            if (space == null) return;

            space.savedVariables.reset();
            sender.sendMessage(Component.text(Translations.get("success.cleared.variables")).color(NamedTextColor.GREEN));
        }, new ArgumentLiteral("variables"));
    }

    private Space getSpace(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Translations.get("error.needs.player")).color(NamedTextColor.RED));
            return null;
        }

        Space space = SpaceManager.getSpaceForPlayer(player);
        if (space == null) {
            sender.sendMessage(Component.text(Translations.get("error.needs.space")).color(NamedTextColor.RED));
            return null;
        }

        if (!space.info.owner.equals(player.getUuid())) {
            sender.sendMessage(Component.text(Translations.get("error.needs.owner")).color(NamedTextColor.RED));
            return null;
        }

        return space;
    }

}
