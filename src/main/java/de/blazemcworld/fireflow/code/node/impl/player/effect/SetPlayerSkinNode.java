package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import de.blazemcworld.fireflow.util.Statistics;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.Material;

public class SetPlayerSkinNode extends Node {

    public SetPlayerSkinNode() {
        super("set_player_skin", Material.CREEPER_HEAD);
        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<String> skin = new Input<>("skin", StringType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);
        signal.onSignal((ctx) -> {
            String str = skin.getValue(ctx);
            PlayerValue pv = player.getValue(ctx);
            Thread.startVirtualThread(() -> {
                PlayerSkin s;
                if (str.contains("-")) s = PlayerSkin.fromUuid(str);
                else if (str.startsWith("eyJ0ZXh0dXJlcyI6eyJTS0lO")) s = new PlayerSkin(str, "");
                else s = PlayerSkin.fromUsername(str);

                if (s != null) {
                    ctx.evaluator.scheduler.scheduleNextTick(() -> {
                        pv.tryUse(ctx, p -> {
                            p.setSkin(s);
                            Statistics.needsSkinReset.put(p, true);
                        });
                    });
                }
            });
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SetPlayerSkinNode();
    }
}