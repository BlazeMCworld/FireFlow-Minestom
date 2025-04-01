package de.blazemcworld.fireflow.code;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.node.Node.Varargs;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionCallNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionDefinition;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionInputsNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionOutputsNode;
import de.blazemcworld.fireflow.code.widget.NodeWidget;
import de.blazemcworld.fireflow.code.widget.Widget;
import de.blazemcworld.fireflow.space.Space;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.timer.Scheduler;

import java.util.*;

public class CodeEvaluator {

    public final Space space;
    public final EventNode<InstanceEvent> events;
    private boolean stopped = false;
    public long tickStart = System.currentTimeMillis();
    public boolean processing = false;
    public final VariableStore sessionVariables = new VariableStore();
    public final Set<Node> nodes;
    public final Scheduler scheduler;
    public final LinkedList<Integer> cpuHistory = new LinkedList<>();
    public final WeakHashMap<CancellableEvent, Boolean> cancelState = new WeakHashMap<>();
    public final Thread spaceThread;

    public CodeEvaluator(Space space) {
        this.space = space;
        scheduler = space.play.scheduler();

        Set<Node> nodes = new HashSet<>();
        for (Widget widget : space.editor.rootWidgets) {
            if (widget instanceof NodeWidget nodeWidget) {
                nodes.add(nodeWidget.node);
            }
        }

        events = EventNode.type("space-" + space.info.id, EventFilter.INSTANCE);
        space.play.eventNode().addChild(events);

        this.nodes = copyNodes(nodes);

        events.addListener(PlayerBlockPlaceEvent.class, this::setupDefaultCancel);
        events.addListener(PlayerBlockBreakEvent.class, this::setupDefaultCancel);

        for (Node node : this.nodes) {
            node.init(this);
        }

        events.addListener(PlayerBlockPlaceEvent.class, this::applyDefaultCancel);
        events.addListener(PlayerBlockBreakEvent.class, this::applyDefaultCancel);

        spaceThread = new Thread(() -> {
            long nextTick = System.currentTimeMillis();
            while (!this.isStopped()) {
                tickStart = System.currentTimeMillis();
                synchronized (cpuHistory) {
                    if (cpuHistory.size() >= 20) cpuHistory.removeFirst();
                    processing = true;
                }
                space.play.spaceTick(nextTick);
                synchronized (cpuHistory) {
                    processing = false;
                    cpuHistory.add((int) (System.currentTimeMillis() - tickStart));
                }
                nextTick += 50;
                long now = System.currentTimeMillis();
                if (now < nextTick) {
                    try {
                        Thread.sleep(nextTick - now);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else if (now + 1000 < nextTick) {
                    long skip = (nextTick - now - 500) / 50;
                    nextTick += skip * 50;
                }
            }
        }, "Space-" + space.info.id);
        spaceThread.setPriority(Thread.MIN_PRIORITY);
        spaceThread.setDaemon(true);
        spaceThread.start();
    }

    private void setupDefaultCancel(CancellableEvent e) {
        cancelState.put(e, true);
    }

    private void applyDefaultCancel(CancellableEvent c) {
        if (cancelState.containsKey(c)) {
            c.setCancelled(cancelState.get(c));
        }
    }

    public void stop() {
        space.play.eventNode().removeChild(events);
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    @SuppressWarnings("unchecked")
    private Set<Node> copyNodes(Set<Node> nodes) {
        HashMap<Node, Node> old2new = new HashMap<>();

        HashMap<String, FunctionDefinition> functions = new HashMap<>();

        for (FunctionDefinition old : space.editor.functions.values()) {
            FunctionDefinition copy = new FunctionDefinition(old.name, old.icon);
            for (Node.Output<?> input : old.inputsNode.outputs) {
                copy.addInput(input.id, input.type);
            }
            for (Node.Input<?> output : old.outputsNode.inputs) {
                copy.addOutput(output.id, output.type);
            }
            functions.put(old.name, copy);
        }

        for (Node node : nodes) {
            Node copy = null;
            
            if (node instanceof FunctionCallNode call) {
                copy = new FunctionCallNode(functions.get(call.function.name));
            }

            if (node instanceof FunctionInputsNode inputsNode) {
                copy = functions.get(inputsNode.function.name).inputsNode;
            }

            if (node instanceof FunctionOutputsNode outputsNode) {
                copy = functions.get(outputsNode.function.name).outputsNode;
            }

            if (copy == null) copy = node.copy();
            
            for (Varargs<?> base : node.varargs) {
                for (Varargs<?> next : copy.varargs) {
                    if (!base.id.equals(next.id)) continue;
                    next.ignoreUpdates = true;
                    copy.inputs.removeAll(next.children);
                    next.children.clear();

                    for (Node.Input<?> input : base.children) {
                        next.addInput(input.id);
                    }
                }
            }

            copy.clonedFrom = node;
            old2new.put(node, copy);
        }

        for (Node old : nodes) {
            Node copy = old2new.get(old);
            for (int i = 0; i < copy.inputs.size(); i++) {
                Node.Input<?> newInput = copy.inputs.get(i);
                Node.Output<?> oldTarget = old.inputs.get(i).connected;
                if (oldTarget == null) continue;
                Node.Output<?> newTarget = old2new.get(oldTarget.getNode()).outputs.get(oldTarget.getNode().outputs.indexOf(oldTarget));
                if (newTarget == null) continue;
                ((Node.Input<Object>) newInput).connect((Node.Output<Object>) newTarget);
            }

            for (int i = 0; i < copy.outputs.size(); i++) {
                Node.Output<?> newOutput = copy.outputs.get(i);
                Node.Input<?> oldTarget = old.outputs.get(i).connected;
                if (oldTarget == null) continue;
                Node.Input<?> newTarget = old2new.get(oldTarget.getNode()).inputs.get(oldTarget.getNode().inputs.indexOf(oldTarget));
                ((Node.Output<Object>) newOutput).connected = (Node.Input<Object>) newTarget;
            }

            for (int i = 0; i < copy.inputs.size(); i++) {
                Node.Input<?> newInput = copy.inputs.get(i);
                Node.Input<?> oldInput = old.inputs.get(i);
                if (oldInput.inset == null) continue;
                newInput.setInset(oldInput.inset);
            }
        }

        return new HashSet<>(old2new.values());
    }

    public CodeThread newCodeThread() {
        return new CodeThread(this);
    }

    public CodeThread newCodeThread(Event event) {
        return new CodeThread(this, event);
    }
}
