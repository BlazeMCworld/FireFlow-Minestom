package de.blazemcworld.fireflow.code.widget;

import de.blazemcworld.fireflow.code.CodeEditor;
import de.blazemcworld.fireflow.code.Interaction;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.node.NodeList;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionCallNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionDefinition;
import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.code.type.WireType;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;

import java.util.*;

public class NodeMenuWidget implements Widget {

    private Widget root;
    public NodeIOWidget ioOrigin;

    public NodeMenuWidget(NodeList.Category category, CodeEditor editor, List<NodeList.Category> parents) {
        VerticalContainerWidget menu = new VerticalContainerWidget();

        if (parents != null && !parents.isEmpty()) {
            NodeList.Category parent = parents.get(parents.size() - 1);
            ButtonWidget button = new ButtonWidget(new TextWidget(Component.text("🠄 Back").color(NamedTextColor.GRAY)));

            button.handler = interaction -> {
                if (interaction.type() != Interaction.Type.RIGHT_CLICK) return false;
                remove();
                interaction.editor().rootWidgets.remove(this);

                NodeMenuWidget n = new NodeMenuWidget(parent, interaction.editor(), parents.subList(0, parents.size() - 1));
                n.setPos(root.getPos());
                n.update(interaction.editor().space.code);
                n.ioOrigin = ioOrigin;
                interaction.editor().rootWidgets.add(n);
                return true;
            };

            menu.widgets.add(button);
        }

        GridWidget grid = new GridWidget(5);
        menu.widgets.add(grid);

        boolean emptyCategory = true;
        for (NodeList.Category subCategory : category.categories) {
            if (subCategory.isFunctions && subCategory.filter != null) {
                boolean hasEntry = false;
                for (FunctionDefinition fn : editor.functions.values()) {
                    FunctionCallNode fnNode = new FunctionCallNode(fn);
                    fn.callNodes.remove(fnNode); // Remove since its not actually a real node
                    if (subCategory.filter.test(fnNode)) {
                        hasEntry = true;
                        break;
                    }
                }
                if (!hasEntry) continue;
            }

            ButtonWidget button = new ButtonWidget(new IconWidget(ItemStack.of(subCategory.icon), subCategory.name, 0.652));

            button.handler = interaction -> {
                if (interaction.type() != Interaction.Type.RIGHT_CLICK) return false;
                remove();
                interaction.editor().rootWidgets.remove(this);

                List<NodeList.Category> path = new ArrayList<>();
                if (parents != null) path.addAll(parents);
                path.add(category);
                NodeMenuWidget n = new NodeMenuWidget(subCategory, interaction.editor(), path);
                n.setPos(root.getPos());
                n.update(interaction.editor().space.code);
                n.ioOrigin = ioOrigin;
                interaction.editor().rootWidgets.add(n);
                return true;
            };

            grid.widgets.add(button);
            emptyCategory = false;
        }

        List<Node> nodes = category.nodes;
        if (category.isFunctions) {
            nodes = new ArrayList<>();
            for (FunctionDefinition fn : editor.functions.values()) {
                FunctionCallNode fnNode = new FunctionCallNode(fn);
                fn.callNodes.remove(fnNode); // Remove since its not actually a real node
                if (category.filter == null || category.filter.test(fnNode)) nodes.add(fnNode);
            }
        }

        for (Node node : nodes) {
            ButtonWidget button = new ButtonWidget(new IconWidget(ItemStack.of(node.icon), node.getTitle(), 0.652));

            button.handler = interaction -> {
                if (interaction.type() != Interaction.Type.RIGHT_CLICK) return false;
                remove();
                interaction.editor().rootWidgets.remove(this);

                createNode(interaction.editor(), interaction.pos(), node, new ArrayList<>(), ioOrigin);
                return true;
            };

            grid.widgets.add(button);
            emptyCategory = false;
        }

        if (emptyCategory) {
            menu.widgets.remove(grid);
            menu.widgets.add(new TextWidget(Component.text("Empty category")));
        }

        BorderWidget<VerticalContainerWidget> border = new BorderWidget<>(menu);
        border.backgroundColor(0xdd000011);
        ButtonWidget button = new ButtonWidget(border);

        button.handler = interaction -> {
            if (interaction.type() == Interaction.Type.LEFT_CLICK) {
                remove();
                interaction.editor().rootWidgets.remove(this);
                return true;
            }
            return false;
        };

        root = button;
    }

    public static void createNode(CodeEditor e, Vec pos, Node node, List<WireType<?>> types, NodeIOWidget ioOrigin) {
        if (node instanceof FunctionCallNode call) {
            if (!e.functions.containsKey(call.function.name)) return;
            if (e.functions.get(call.function.name) != call.function) return;
        }
        if (node.getTypeCount() > types.size()) {
            List<WireType<?>> filtered = new ArrayList<>();
            for (WireType<?> type : AllTypes.all) {
                if (node.acceptsType(type, types.size())) {
                    filtered.add(type);
                }
            }

            TypeSelectorWidget selector = new TypeSelectorWidget(filtered, type -> {
                types.add(type);
                createNode(e, pos, node.copyWithTypes(types), types, ioOrigin);
            });
            selector.setPos(pos.add(selector.getSize().mul(4).apply(Vec.Operator.CEIL).div(8)));
            selector.update(e.space.code);
            e.rootWidgets.add(selector);
            return;
        }

        NodeWidget n;
        if (types.isEmpty()) {
            n = new NodeWidget(node.copy(), e.space.editor);
        } else {
            n = new NodeWidget(node.copyWithTypes(types), e.space.editor);
        }

        Vec s = n.getSize();
        if (ioOrigin == null || !ioOrigin.parent.getIOWidgets().contains(ioOrigin) || !e.rootWidgets.contains(ioOrigin.parent)) {
            n.setPos(pos.add(Math.round(s.x() * 4) / 8f, Math.round(s.y() * 4) / 8f, 0));
        } else {
            if (ioOrigin.isInput()) {
                n.setPos(ioOrigin.getPos().add(0.75 + s.x(), 0, 0));
            } else {
                n.setPos(ioOrigin.getPos().sub(ioOrigin.getSize().x() + 0.75, 0, 0));
            }

            n.update(e.space.code);
            Pair<Node.Input<?>, Node.Output<?>> compatible = getCompatible(n.node, ioOrigin);
            if (compatible != null) {
                if (compatible.first() != null && !ioOrigin.isInput()) {
                    for (NodeIOWidget io : n.getInputs()) {
                        if (io.input != compatible.first()) continue;
                        double yDiff = io.getPos().y() - ioOrigin.getPos().y();
                        n.setPos(n.getPos().sub(0, yDiff, 0));
                        resolveOverlap(e, n);
                        n.update(e.space.code);
                        pair(io, ioOrigin, e);
                        break;
                    }
                } else if (compatible.second() != null && ioOrigin.isInput()) {
                    for (NodeIOWidget io : n.getOutputs()) {
                        if (io.output != compatible.second()) continue;
                        double yDiff = io.getPos().y() - ioOrigin.getPos().y();
                        n.setPos(n.getPos().sub(0, yDiff, 0));
                        resolveOverlap(e, n);
                        n.update(e.space.code);
                        pair(ioOrigin, io, e);
                        break;
                    }
                }
            }

            for (NodeIOWidget io : n.getInputs()) {
                if (io.input.inset != null || io.input.connected != null) continue;
                List<NodeWidget> todo = new LinkedList<>();
                todo.add(ioOrigin.parent);
                Set<NodeWidget> checked = new HashSet<>();
                checked.add(n);

                connect:
                for (int i = 0; i < 16; i++) {
                    if (todo.isEmpty()) break;
                    NodeWidget w = todo.removeFirst();
                    checked.add(w);
                    for (NodeIOWidget other : w.getOutputs()) {
                        if (other.type() != io.type()) continue;
                        if (AllTypes.isValue(io.type()) || other.connections.isEmpty()) {
                            pair(io, other, e);
                            break connect;
                        }
                    }

                    if (checked.size() + todo.size() >= 16) continue;

                    for (NodeIOWidget in : w.getIOWidgets()) {
                        for (WireWidget c : in.connections) {
                            for (NodeIOWidget io2 : c.getInputs()) {
                                if (checked.contains(io2.parent)) continue;
                                if (todo.contains(io2.parent)) continue;
                                todo.add(io2.parent);
                            }
                            for (NodeIOWidget io2 : c.getOutputs()) {
                                if (checked.contains(io2.parent)) continue;
                                if (todo.contains(io2.parent)) continue;
                                todo.add(io2.parent);
                            }
                        }
                    }
                }
            }
        }
        n.update(e.space.code);
        e.rootWidgets.add(n);
    }

    public static Pair<Node.Input<?>, Node.Output<?>> getCompatible(Node node, NodeIOWidget io) {
        Pair<Node.Input<?>, Node.Output<?>> compatible = null;
        if (io.isInput()) {
            for (Node.Output<?> out : node.outputs) {
                if (io.input.type == out.type) return Pair.of(null, out);
                if (isCompatible(io.input, out)) compatible = Pair.of(null, out);
            }
        } else {
            for (Node.Input<?> in : node.inputs) {
                if (io.output.type == in.type) return Pair.of(in, null);
                if (isCompatible(in, io.output)) compatible = Pair.of(in, null);
            }
        }
        return compatible;
    }

    private static void pair(NodeIOWidget input, NodeIOWidget output, CodeEditor editor) {
        Vec start = output.getPos().sub(output.getSize().sub(0, 1 / 8f, 0));
        Vec end = input.getPos().add(-1 / 8f, -1 / 8f, 0);

        List<WireWidget> wires = new ArrayList<>();
        List<Vec> path = editor.pathfinder.findPath(start.sub(0.375, 0, 0), end.add(0.375, 0, 0));
        path.addFirst(start);
        path.add(end);

        for (int i = 0; i < path.size() - 1; i++) {
            wires.add(new WireWidget(path.get(i).withZ(15.999), output.type(), path.get(i + 1).withZ(15.999)));
        }

        for (int i = 1; i < wires.size(); i++) {
            wires.get(i - 1).connectNext(wires.get(i));
        }
        wires.getFirst().connectPrevious(output);
        wires.getLast().connectNext(input);
        output.connect(wires.getFirst());
        input.connect(wires.getLast());

        for (WireWidget wire : wires) {
            editor.rootWidgets.add(wire);
            wire.update(editor.space.code);
        }
        wires.getFirst().cleanup(editor);
    }

    private static void resolveOverlap(CodeEditor editor, NodeWidget node) {
        Vec pos = node.getPos();
        Vec size = node.getSize();
        Set<NodeWidget> relevant = new HashSet<>();
        for (Widget w : editor.rootWidgets) {
            if (!(w instanceof NodeWidget other)) continue;
            if (other == node) continue;
            Vec otherPos = other.getPos();
            Vec otherSize = other.getSize();

            if (otherPos.y() - otherSize.y() - 0.25 > pos.y()) continue;
            if (otherPos.x() < pos.x() - size.x() || pos.x() < otherPos.x() - otherSize.x()) continue;

            relevant.add(other);
        }

        for (int i = 0; i < 16; i++) {
            boolean neededAdjustment = false;
            for (NodeWidget other : relevant) {
                Vec otherPos = other.getPos();
                Vec otherSize = other.getSize();

                if (otherPos.y() - otherSize.y() - 0.25 > pos.y()) continue;
                if (pos.y() - size.y() - 0.25 > otherPos.y()) continue;

                neededAdjustment = true;
                pos = pos.withY(otherPos.y() - size.y() - 0.5);
            }
            if (!neededAdjustment) break;
        }

        node.setPos(pos);
    }

    private static boolean isCompatible(Node.Input<?> in, Node.Output<?> out) {
        return out.type == null || in.type == null || out.type == in.type || in.type.canConvert(out.type);
    }

    @Override
    public void setPos(Vec pos) {
        root.setPos(pos.withZ(15.99));
    }

    @Override
    public Vec getPos() {
        return root.getPos();
    }

    @Override
    public Vec getSize() {
        return root.getSize();
    }

    @Override
    public void update(InstanceContainer inst) {
        root.update(inst);
    }

    @Override
    public void remove() {
        root.remove();
    }

    @Override
    public boolean interact(Interaction i) {
        return root.interact(i);
    }

    @Override
    public Widget getWidget(Vec pos) {
        if (!inBounds(pos)) return null;
        return root.getWidget(pos);
    }

    @Override
    public List<Widget> getChildren() {
        return List.of(root);
    }

    @Override
    public int interactionPriority() {
        return 1;
    }
}
