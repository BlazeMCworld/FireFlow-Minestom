package de.blazemcworld.fireflow.code.node.impl.flow;

import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.NumberType;
import de.blazemcworld.fireflow.code.type.SignalType;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleNode extends Node {

    public ScheduleNode() {
        super("schedule", Material.CLOCK);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<Double> delay = new Input<>("delay", NumberType.INSTANCE);

        Output<Void> now = new Output<>("now", SignalType.INSTANCE);
        Output<Void> task = new Output<>("task", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            AtomicInteger remaining = new AtomicInteger(delay.getValue(ctx).intValue());

            CodeThread spawned = ctx.subThread();
            ctx.evaluator.scheduler.submitTask(() -> {
                if (ctx.evaluator.isStopped()) return TaskSchedule.stop();
                if (remaining.getAndDecrement() <= 0) {
                    spawned.sendSignal(task);
                    spawned.clearQueue();
                    return TaskSchedule.stop();
                }
                return TaskSchedule.tick(1);
            });

            ctx.sendSignal(now);
        });
    }

    @Override
    public Node copy() {
        return new ScheduleNode();
    }
}
