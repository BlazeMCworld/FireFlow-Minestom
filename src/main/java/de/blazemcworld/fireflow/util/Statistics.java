package de.blazemcworld.fireflow.util;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.AttributeInstance;

import java.util.WeakHashMap;

public class Statistics {

    public static final WeakHashMap<Player, Boolean> needsSkinReset = new WeakHashMap<>();

    public static void reset(Player player) {
        player.closeInventory();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);
        player.setFood(20);
        player.setFoodSaturation(20f);
        player.setInvulnerable(false);
        player.setHealth(20);
        player.setArrowCount(0);
        player.setFireTicks(0);
        player.setGlowing(false);
        player.setInvisible(false);
        if (needsSkinReset.containsKey(player)) {
            player.setSkin(PlayerSkin.fromUuid(player.getUuid().toString()));
            needsSkinReset.remove(player);
        }
        for (AttributeInstance attr : player.getAttributes()) {
            attr.clearModifiers();
        }
    }

}
