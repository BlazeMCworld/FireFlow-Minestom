package de.blazemcworld.fireflow.command;

import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentItemStack;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

public class FunctionCommand extends Command {
    
    public FunctionCommand() {
        super("function");

        addSyntax((sender, ctx) -> {
            run(sender, "create", ctx.get("name"));
        }, new ArgumentLiteral("create"), new ArgumentString("name"));

        addSyntax((sender, ctx) -> {
            run(sender, "delete", null);
        }, new ArgumentLiteral("delete"));

        addSyntax((sender, ctx) -> {
            run(sender, "add_input", ctx.get("name"));
        }, new ArgumentLiteral("add"), new ArgumentLiteral("input"), new ArgumentString("name"));

        addSyntax((sender, ctx) -> {
            run(sender, "add_output", ctx.get("name"));
        }, new ArgumentLiteral("add"), new ArgumentLiteral("output"), new ArgumentString("name"));

        addSyntax((sender, ctx) -> {
            run(sender, "remove_input", ctx.get("name"));
        }, new ArgumentLiteral("remove"), new ArgumentLiteral("input"), new ArgumentString("name"));

        addSyntax((sender, ctx) -> {
            run(sender, "remove_output", ctx.get("name"));
        }, new ArgumentLiteral("remove"), new ArgumentLiteral("output"), new ArgumentString("name"));

        addSyntax((sender, ctx) -> {
            run(sender, "icon", ctx.<ItemStack>get("item").material().key().value());
        }, new ArgumentLiteral("icon"), new ArgumentItemStack("item"));
    }

    private void run(CommandSender sender, String action, String input) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Translations.get("error.needs.player")).color(NamedTextColor.RED));
            return;
        }
        Space space = SpaceManager.getSpaceForPlayer(player);
        if (space == null) {
            sender.sendMessage(Component.text(Translations.get("error.needs.space")).color(NamedTextColor.RED));
            return;
        }

        if (space.code != player.getInstance()) {
            sender.sendMessage(Component.text(Translations.get("error.mode.code")).color(NamedTextColor.RED));
            return;
        }

        if (action.equals("create")) {
            space.editor.createFunction(player, input);
        } else if (action.equals("delete")) {
            space.editor.deleteFunction(player);
        } else if (action.equals("add_input")) {
            space.editor.addFunctionInput(player, input);
        } else if (action.equals("add_output")) {
            space.editor.addFunctionOutput(player, input);
        } else if (action.equals("remove_input")) {
            space.editor.removeFunctionInput(player, input);
        } else if (action.equals("remove_output")) {
            space.editor.removeFunctionOutput(player, input);
        } else if (action.equals("icon")) {
            space.editor.setFunctionIcon(player, input);
        }
    }

}
