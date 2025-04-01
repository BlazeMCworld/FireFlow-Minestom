package de.blazemcworld.fireflow.code.node.impl.event.player;

import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import de.blazemcworld.fireflow.util.PlayerExitInstanceEvent;
import net.minestom.server.item.Material;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OnPlayerLeaveNode extends Node {

    private final Output<Void> signal;
    private final Output<PlayerValue> player;

    public OnPlayerLeaveNode() {
        super("on_player_leave", Material.IRON_DOOR);

        signal = new Output<>("signal", SignalType.INSTANCE);
        player = new Output<>("player", PlayerType.INSTANCE);
        player.valueFromScope();
    }

    @Override
    public void init(CodeEvaluator evaluator) {
        evaluator.events.addListener(PlayerExitInstanceEvent.class, event -> {
            CodeThread thread = evaluator.newCodeThread(event);
            thread.setScopeValue(player, new PlayerValue(event.getPlayer()));
            thread.sendSignal(signal);
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (Thread.currentThread() == evaluator.spaceThread) {
                Thread.startVirtualThread(() -> {
                    thread.clearQueue();
                    future.complete(null);
                });
            } else {
                evaluator.scheduler.scheduleNextProcess(() -> {
                    thread.clearQueue();
                    future.complete(null);
                });
            }
            try {
                future.get(10, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException ignored) {}
        });
    }

    @Override
    public Node copy() {
        return new OnPlayerLeaveNode();
    }
}
