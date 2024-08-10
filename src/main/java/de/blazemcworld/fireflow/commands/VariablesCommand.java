package de.blazemcworld.fireflow.commands;

import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Messages;
import de.blazemcworld.fireflow.value.MessageValue;
import de.blazemcworld.fireflow.value.PlayerValue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VariablesCommand extends Command {
    public VariablesCommand() {
        super("variables", "vars");

        setDefaultExecutor((sender, ctx) -> showVariables(sender, ""));
        addSyntax((sender, ctx) -> {
            showVariables(sender, ctx.<String>get("filter").toLowerCase());
        }, new ArgumentString("filter"));
    }

    private void showVariables(CommandSender sender, String filter) {
        if (sender instanceof Player player) {
            Space space = SpaceManager.getSpace(player);
            if (space == null) {
                sender.sendMessage(Messages.error("You must be in a space to do this!"));
                return;
            }
            if (!space.info.owner.equals(player.getUuid())) {
                boolean allowed = false;
                for (UUID contributor : space.info.contributors) {
                    if (contributor.equals(player.getUuid())) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    sender.sendMessage(Messages.error("You are not allowed to do that!"));
                    return;
                }
            }

            int count = 0;
            for (Map.Entry<String, Object> variable : space.variables.entrySet()) {
                if (variable.getKey().toLowerCase().contains(filter)) {
                    sender.sendMessage(Component.text(variable.getKey()).color(NamedTextColor.GREEN)
                            .append(Component.text(" - ").color(NamedTextColor.GRAY))
                            .append(Component.text(stringify(variable.getValue())).color(NamedTextColor.GREEN))
                    );
                    count++;
                    if (count > 100) {
                        sender.sendMessage(Messages.error("Stopped searching after finding 100 variables."));
                        return;
                    }
                }
            }
            sender.sendMessage(Messages.success("Found " + count + " space variable" + (count == 1 ? "" : "s") + "!"));
        } else {
            sender.sendMessage(Messages.error("Only players can do this!"));
        }
    }

    private String stringify(Object value) {
        if (value instanceof List<?> l) {
            return "List (" + l.size() + " values)";
        }
        if (value instanceof PlayerValue.Reference p) {
            Player player = p.resolve();
            if (p == null) {
                return "Offline Player (" + p.uuid() + ")";
            } else {
                return player.getUsername() + " (" + p.uuid() + ")";
            }
        }
        if (value instanceof Component c) {
            return MessageValue.MM.serialize(c);
        }
        return String.valueOf(value);
    }
}
