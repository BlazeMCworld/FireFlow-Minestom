package de.blazemcworld.fireflow.code.node.impl.event.player;

import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.item.Material;

public class OnPlayerJoinNode extends Node {

    private final Output<Void> signal;
    private final Output<PlayerValue> player;

    public OnPlayerJoinNode() {
        super("on_player_join", Material.OAK_DOOR);

        signal = new Output<>("signal", SignalType.INSTANCE);
        player = new Output<>("player", PlayerType.INSTANCE);
        player.valueFromScope();
    }

    @Override
    public void init(CodeEvaluator evaluator) {
        evaluator.events.addListener(PlayerSpawnEvent.class, event -> {
            CodeThread thread = evaluator.newCodeThread(event);
            thread.setScopeValue(player, new PlayerValue(event.getPlayer()));
            thread.sendSignal(signal);
            evaluator.scheduler.scheduleNextProcess(thread::clearQueue);
        });
    }

    @Override
    public Node copy() {
        return new OnPlayerJoinNode();
    }
}
