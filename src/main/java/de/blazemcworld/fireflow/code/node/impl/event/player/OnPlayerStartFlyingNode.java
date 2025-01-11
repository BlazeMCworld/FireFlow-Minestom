package de.blazemcworld.fireflow.code.node.impl.event.player;

import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.item.Material;

public class OnPlayerStartFlyingNode extends Node {

    private final Output<Void> signal;
    private final Output<PlayerValue> player;

    public OnPlayerStartFlyingNode() {
        super("on_player_start_flying", Material.FEATHER);

        signal = new Output<>("signal", SignalType.INSTANCE);
        player = new Output<>("player", PlayerType.INSTANCE);
        player.valueFromThread();
    }

    @Override
    public void init(CodeEvaluator evaluator) {
        evaluator.events.addListener(PlayerStartFlyingEvent.class, event -> {
            CodeThread thread = evaluator.newCodeThread(event);
            thread.setThreadValue(player, new PlayerValue(event.getPlayer()));
            thread.sendSignal(signal);
            thread.clearQueue();
        });
    }

    @Override
    public Node copy() {
        return new OnPlayerStartFlyingNode();
    }

}
