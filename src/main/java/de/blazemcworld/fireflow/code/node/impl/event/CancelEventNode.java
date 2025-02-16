package de.blazemcworld.fireflow.code.node.impl.event;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ConditionType;
import de.blazemcworld.fireflow.code.type.SignalType;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.item.Material;

public class CancelEventNode extends Node {

    public CancelEventNode() {
        super("cancel_event", Material.BARRIER);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<Boolean> cancel = new Input<>("cancel", ConditionType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);
        Output<Void> failed = new Output<>("failed", SignalType.INSTANCE);
        signal.onSignal((ctx) -> {
            Event event = ctx.event;
            boolean cancels = cancel.getValue(ctx);
            if (!(event instanceof CancellableEvent e)) {
                ctx.sendSignal(failed);
                return;
            }
            if (ctx.evaluator.cancelState.containsKey(e)) {
                if (e.isCancelled() == ctx.evaluator.cancelState.get(e)) {
                    ctx.sendSignal(failed);
                    return;
                }
                ctx.evaluator.cancelState.put(e, cancels);
            } else {
                if (e.isCancelled() == cancels) {
                    ctx.sendSignal(failed);
                    return;
                }
                e.setCancelled(cancels);
            }
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new CancelEventNode();
    }
}
