package de.blazemcworld.fireflow.code.node.impl.event.player;

import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.VectorType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.item.Material;

public class OnPlayerBreakBlockNode extends Node {

    private final Output<Void> signal;
    private final Output<PlayerValue> player;
    private final Output<Vec> position;

    public OnPlayerBreakBlockNode() {
        super("on_player_break_block", Material.DIAMOND_PICKAXE);

        signal = new Output<>("signal", SignalType.INSTANCE);
        player = new Output<>("player", PlayerType.INSTANCE);
        position = new Output<>("position", VectorType.INSTANCE);
        player.valueFromScope();
        position.valueFromScope();
    }

    @Override
    public void init(CodeEvaluator evaluator) {
        evaluator.events.addListener(PlayerBlockBreakEvent.class, event -> {
            CodeThread thread = evaluator.newCodeThread(event);
            thread.setScopeValue(player, new PlayerValue(event.getPlayer()));
            thread.setScopeValue(position, event.getBlockPosition().asVec());
            thread.sendSignal(signal);
            thread.clearQueue();
        });
    }

    @Override
    public Node copy() {
        return new OnPlayerBreakBlockNode();
    }
}

