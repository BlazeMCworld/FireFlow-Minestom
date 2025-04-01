package de.blazemcworld.fireflow.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public class Transfer {

    public static void move(Player player, Instance destination) {
        MinecraftServer.getGlobalEventHandler().call(new PlayerExitInstanceEvent(player));
        player.respawn();
        Statistics.reset(player);
        if (player.getInstance() == destination) {
            player.teleport(Pos.ZERO).join();
        } else {
            player.setInstance(destination, Pos.ZERO).join();
        }
    }

}
