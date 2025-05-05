package de.blazemcworld.fireflow.code;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.blazemcworld.fireflow.FireFlow;
import de.blazemcworld.fireflow.code.action.Action;
import de.blazemcworld.fireflow.code.action.DeleteSelectAction;
import de.blazemcworld.fireflow.code.action.SelectAction;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.node.NodeList;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionCallNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionDefinition;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionInputsNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionOutputsNode;
import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.code.widget.*;
import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.util.PlayerExitInstanceEvent;
import de.blazemcworld.fireflow.util.Translations;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.Material;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CodeEditor {

    public final Space space;
    public final Set<Widget> rootWidgets = new HashSet<>();
    public final HashMap<Player, Set<Widget>> lockedWidgets = new HashMap<>();
    private final HashMap<Player, Action> actions = new HashMap<>();
    private final Path codePath;
    public final HashMap<String, FunctionDefinition> functions = new HashMap<>();
    public final Pathfinder pathfinder = new Pathfinder(this);

    public CodeEditor(Space space) {
        this.space = space;
        codePath = Path.of("spaces/" + space.info.id + "/code.json");

        EventNode<InstanceEvent> events = space.code.eventNode();

        events.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();

            player.setAllowFlying(true);
            player.setFlying(true);

            Entity interactionHelper = new Entity(EntityType.INTERACTION);
            interactionHelper.setNoGravity(true);
            InteractionMeta meta = (InteractionMeta) interactionHelper.getEntityMeta();
            meta.setWidth(-0.5f);
            meta.setHeight(-0.5f);
            interactionHelper.setInstance(event.getInstance(), Pos.ZERO);
            player.addPassenger(interactionHelper);

            lockedWidgets.put(player, new HashSet<>());
        });

        events.addListener(PlayerExitInstanceEvent.class, event -> {
            for (Entity passenger : event.getEntity().getPassengers()) {
                if (passenger.getEntityType() == EntityType.INTERACTION) passenger.remove();
            }

            if (actions.containsKey(event.getPlayer())) actions.get(event.getPlayer()).stop(this, event.getPlayer());
            actions.remove(event.getPlayer());
            lockedWidgets.remove(event.getPlayer());
        });

        events.addListener(PlayerEntityInteractEvent.class, event -> {
            handleInteraction(event.getPlayer(), Interaction.Type.RIGHT_CLICK);
        });
        events.addListener(PlayerSwapItemEvent.class, event -> {
            handleInteraction(event.getPlayer(), Interaction.Type.SWAP_HANDS);
        });
        events.addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player player) {
                handleInteraction(player, Interaction.Type.LEFT_CLICK);
            }
        });

        events.addListener(PlayerTickEvent.class, event -> {
            Action a = actions.get(event.getPlayer());
            if (a == null) return;
            Vec cursor = getCursor(event.getPlayer());
            a.tick(cursor, this, event.getPlayer());
        });

        events.addListener(PlayerChatEvent.class, event -> {
            Vec pos = getCursor(event.getPlayer()).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);
            for (Widget w : rootWidgets) {
                if (w.getWidget(pos) instanceof NodeIOWidget input) {
                    if (!input.isInput()) return;
                    event.setCancelled(true);
                    if (isLocked(w) != null && !isLockedByPlayer(w, event.getPlayer())) {
                        event.getPlayer().sendMessage(Component.text(Translations.get("error.locked", isLocked(w).getUsername())).color(NamedTextColor.RED));
                        return;
                    }
                    if (input.type().parseInset(event.getRawMessage()) == null) {
                        event.getPlayer().sendMessage(Component.text(Translations.get("error.invalid.inset")).color(NamedTextColor.RED));
                        return;
                    }

                    input.insetValue(event.getRawMessage().replaceAll("(?<!\\\\)\\(space\\)", " "), this);
                    input.update(space.code);
                    w.update(space.code);
                    return;
                }
            }
        });

        load();
    }

    private void handleInteraction(Player player, Interaction.Type type) {
        Vec pos = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);
        Interaction i = new Interaction(this, player, pos, type);

        if (actions.containsKey(player)) {
            actions.get(player).interact(i);
            return;
        }

        List<Widget> hovered = new ArrayList<>();
        for (Widget w : new HashSet<>(rootWidgets)) {
            if (w.inBounds(i.pos())) hovered.add(w);
        }
        if (!hovered.isEmpty()) {
            hovered.sort(Comparator.comparing(w -> -w.interactionPriority()));
            for (Widget w : hovered) {
                if (isLocked(w) != null && !isLockedByPlayer(w, player)) {
                    player.sendMessage(Component.text(Translations.get("error.locked", isLocked(w).getUsername())).color(NamedTextColor.RED));
                    return;
                }
                if (w.interact(i)) {
                    return;
                }
            }
        }

        if (type == Interaction.Type.RIGHT_CLICK) {
            actions.put(player, new SelectAction(pos));
        } else if (type == Interaction.Type.SWAP_HANDS) {
            NodeMenuWidget n = new NodeMenuWidget(NodeList.root, this, null);
            Vec s = n.getSize();
            n.setPos(pos.add(Math.round(s.x() * 4) / 8f, Math.round(s.y() * 4) / 8f, 0));
            n.update(space.code);
            rootWidgets.add(n);
        } else if (type == Interaction.Type.LEFT_CLICK) {
            actions.put(player, new DeleteSelectAction(pos));
        }
    }

    public void addNode(Player player, String query, boolean isSearch) {
        String lowerQuery = query.toLowerCase();
        Vec pos = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);

        if (isSearch) {
            NodeMenuWidget n = new NodeMenuWidget(NodeList.root.filtered((node) -> node.getTitle().toLowerCase().contains(lowerQuery)), this, null);
            Vec s = n.getSize();
            n.setPos(pos.add(Math.round(s.x() * 4) / 8f, Math.round(s.y() * 4) / 8f, 0));
            n.update(space.code);
            rootWidgets.add(n);
        } else {
            Node node = FuzzySearch.extractOne(query, NodeList.root.collectNodes(), n -> n.getTitle().toLowerCase()).getReferent();
            NodeMenuWidget.createNode(this, pos, node, new ArrayList<>(), null);
        }
    }

    private Vec getCursor(Player player) {
        double norm = player.getPosition().direction().dot(new Vec(0, 0, -1));
        if (norm >= 0) return Vec.ZERO.withZ(15.999);
        Vec start = player.getPosition().asVec().add(0.0, player.getEyeHeight(), -16);
        double dist = -start.dot(new Vec(0, 0, -1)) / norm;
        if (dist < 0) return Vec.ZERO.withZ(15.999);
        Vec out = start.add(player.getPosition().direction().mul(dist)).withZ(15.999);
        if (out.y() > 99999) out = out.withY(99999);
        if (out.y() < -99999) out = out.withY(-99999);
        if (out.x() > 99999) out = out.withX(99999);
        if (out.x() < -99999) out = out.withX(-99999);
        return out;
    }

    public void unlockWidget(Widget widget, Player player) {
        lockedWidgets.computeIfAbsent(player, p -> new HashSet<>()).remove(widget);
    }

    public void unlockWidgets(List<Widget> widgets, Player player) {
        widgets.forEach(lockedWidgets.computeIfAbsent(player, p -> new HashSet<>())::remove);
    }

    public void unlockWidgets(Player player) {
        lockedWidgets.remove(player);
    }

    public List<Widget> lockWidgets(List<Widget> widgets, Player player) {
        List<Widget> failed = new ArrayList<>();
        for (Widget widget : widgets) {
            if (!lockWidget(widget, player)) failed.add(widget);
        }
        return failed;
    }

    public boolean lockWidget(Widget widget, Player player) {
        Player widgetLockedBy = isLocked(widget);
        if (widgetLockedBy != null) return widgetLockedBy == player;
        lockedWidgets.computeIfAbsent(player, p -> new HashSet<>()).add(widget);
        return true;
    }

    public Player isLocked(Widget widget) {
        for (Map.Entry<Player, Set<Widget>> entry : lockedWidgets.entrySet()) {
            if (entry.getValue().contains(widget)) return entry.getKey();
        }
        return null;
    }

    public boolean isLockedByPlayer(Widget widget, Player player) {
        return lockedWidgets.containsKey(player) && lockedWidgets.get(player).contains(widget);
    }

    public void setAction(Player player, Action action) {
        if (actions.containsKey(player)) {
            actions.get(player).stop(this, player);
        }
        actions.put(player, action);
    }

    public void stopAction(Player player) {
        if (actions.containsKey(player)) {
            actions.get(player).stop(this, player);
        }
        actions.remove(player);
    }

    public void createFunction(Player player, String name) {
        if (functions.containsKey(name)) {
            player.sendMessage(Component.text(Translations.get("error.function.exists")).color(NamedTextColor.RED));
            return;
        }

        FunctionDefinition function = new FunctionDefinition(name, Material.COMMAND_BLOCK);
        functions.put(name, function);

        Vec pos = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);

        NodeWidget inputs = new NodeWidget(function.inputsNode, this);
        NodeWidget outputs = new NodeWidget(function.outputsNode, this);

        inputs.setPos(pos.add(inputs.getSize().x(), 0, 0));
        inputs.update(space.code);
        rootWidgets.add(inputs);
        
        outputs.setPos(pos.sub(outputs.getSize().x(), 0, 0));
        outputs.update(space.code);
        rootWidgets.add(outputs);
    }

    private FunctionDefinition tryGetFunction(Player player) {
        Vec pos = getCursor(player);
        FunctionDefinition function = null;
        for (Widget w : new HashSet<>(rootWidgets)) {
            if (w instanceof NodeWidget nodeWidget && nodeWidget.inBounds(pos)) {
                if (nodeWidget.node instanceof FunctionInputsNode inputsNode) {
                    function = inputsNode.function;

                    for (Node.Output<?> output : function.outputsNode.outputs) {
                        if (output.connected == null) continue;
                        player.sendMessage(Component.text(Translations.get("error.function.in_use")).color(NamedTextColor.RED));
                        return null;
                    }
                } else if (nodeWidget.node instanceof FunctionOutputsNode outputsNode) {
                    function = outputsNode.function;

                    for (Node.Input<?> input : function.inputsNode.inputs) {
                        if (input.connected == null) continue;
                        player.sendMessage(Component.text(Translations.get("error.function.in_use")).color(NamedTextColor.RED));
                        return null;
                    }
                }
            }
        }

        if (function == null) {
            player.sendMessage(Component.text(Translations.get("error.needs.function")).color(NamedTextColor.RED));
            return null;
        }

        if (!function.callNodes.isEmpty()) {
            player.sendMessage(Component.text(Translations.get("error.function.in_use")).color(NamedTextColor.RED));
            return null;
        }

        return function;
    }

    private void refreshFunctionWidgets(FunctionDefinition oldFunction, FunctionDefinition newFunction) {
        for (Widget w : new HashSet<>(rootWidgets)) {
            if (w instanceof NodeWidget old) {
                if (old.node instanceof FunctionInputsNode inputsNode && inputsNode.function == oldFunction) {
                    old.remove();
                    rootWidgets.remove(old);

                    NodeWidget updated = new NodeWidget(newFunction.inputsNode, this);
                    updated.setPos(old.getPos());
                    updated.update(space.code);
                    rootWidgets.add(updated);
                } else if (old.node instanceof FunctionOutputsNode outputsNode && outputsNode.function == oldFunction) {
                    old.remove();
                    rootWidgets.remove(old);

                    NodeWidget updated = new NodeWidget(newFunction.outputsNode, this);
                    updated.setPos(old.getPos());
                    updated.update(space.code);
                    rootWidgets.add(updated);
                }
            }
        }
    }

    public void deleteFunction(Player player) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        functions.remove(function.name);
        for (Widget w : new HashSet<>(rootWidgets)) {
            if (w instanceof NodeWidget nodeWidget) {
                if (nodeWidget.node instanceof FunctionInputsNode inputsNode && inputsNode.function == function) {
                    nodeWidget.remove(this);
                    rootWidgets.remove(nodeWidget);
                } else if (nodeWidget.node instanceof FunctionOutputsNode outputsNode && outputsNode.function == function) {
                    nodeWidget.remove(this);
                    rootWidgets.remove(nodeWidget);
                }
            }
        }
    }

    public void addFunctionInput(Player player, String name) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        if (function.getInput(name) != null) {
            player.sendMessage(Component.text(Translations.get("error.input.exists")).color(NamedTextColor.RED));
            return;
        }

        TypeSelectorWidget typeSelectorWidget = new TypeSelectorWidget(List.copyOf(AllTypes.all), type -> {
            if (function.getInput(name) != null) return;

            function.addInput(name, type);
            refreshFunctionWidgets(function, function);
        });
        typeSelectorWidget.setPos(getCursor(player));
        typeSelectorWidget.update(space.code);
        rootWidgets.add(typeSelectorWidget);
    }

    public void addFunctionOutput(Player player, String name) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        if (function.getOutput(name) != null) {
            player.sendMessage(Component.text(Translations.get("error.output.exists")).color(NamedTextColor.RED));
            return;
        }

        TypeSelectorWidget typeSelectorWidget = new TypeSelectorWidget(List.copyOf(AllTypes.all), type -> {
            if (function.getOutput(name) != null) return;

            function.addOutput(name, type);
            refreshFunctionWidgets(function, function);
        });
        typeSelectorWidget.setPos(getCursor(player));
        typeSelectorWidget.update(space.code);
        rootWidgets.add(typeSelectorWidget);
    }

    public void removeFunctionInput(Player player, String name) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        if (function.getInput(name) == null) {
            player.sendMessage(Component.text(Translations.get("error.input.not_found")).color(NamedTextColor.RED));
            return;
        }

        FunctionDefinition adjusted = new FunctionDefinition(function.name, function.icon);
        for (Node.Output<?> input : function.inputsNode.outputs) {
            if (input.id.equals(name)) continue;
            adjusted.addInput(input.id, input.type);
        }
        for (Node.Input<?> output : function.outputsNode.inputs) {
            adjusted.addOutput(output.id, output.type);
        }
        functions.put(function.name, adjusted);
        refreshFunctionWidgets(function, adjusted);
    }

    public void removeFunctionOutput(Player player, String name) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        if (function.getOutput(name) == null) {
            player.sendMessage(Component.text(Translations.get("error.output.not_found")).color(NamedTextColor.RED));
            return;
        }

        FunctionDefinition adjusted = new FunctionDefinition(function.name, function.icon);
        for (Node.Output<?> input : function.inputsNode.outputs) {
            adjusted.addInput(input.id, input.type);
        }
        for (Node.Input<?> output : function.outputsNode.inputs) {
            if (output.id.equals(name)) continue;
            adjusted.addOutput(output.id, output.type);
        }
        functions.put(function.name, adjusted);
        refreshFunctionWidgets(function, adjusted);
    }

    public void setFunctionIcon(Player player, String icon) {
        FunctionDefinition function = tryGetFunction(player);
        if (function == null) return;

        Material m = Material.fromKey(icon);
        
        if (m == null) {
            player.sendMessage(Component.text(Translations.get("error.unknown.item")).color(NamedTextColor.RED));
            return;
        }
        
        FunctionDefinition adjusted = new FunctionDefinition(function.name, m);
        for (Node.Output<?> input : function.inputsNode.outputs) {
            adjusted.addInput(input.id, input.type);
        }
        for (Node.Input<?> output : function.outputsNode.inputs) {
            adjusted.addOutput(output.id, output.type);
        }
        functions.put(function.name, adjusted);
        refreshFunctionWidgets(function, adjusted);
    }

    public void createSnippet(Player player) {
        Set<NodeWidget> nodes = new HashSet<>();
        Set<WireWidget> wires = new HashSet<>();
        Set<FunctionDefinition> functions = new HashSet<>();
        Set<NodeWidget> todo = new HashSet<>();

        Vec cursor = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);
        for (Widget w : rootWidgets) {
            if (!(w instanceof NodeWidget n)) continue;
            if (w.inBounds(cursor)) {
                todo.add(n);
                nodes.add(n);
            }
        }

        while (!todo.isEmpty()) {
            NodeWidget n = todo.iterator().next();
            todo.remove(n);

            for (NodeIOWidget io : n.getIOWidgets()) {
                for (WireWidget wire : io.connections) {
                    wires.add(wire);
                    for (NodeIOWidget other : wire.getInputs()) {
                        if (nodes.contains(other.parent)) continue;
                        nodes.add(other.parent);
                        todo.add(other.parent);
                    }
                    for (NodeIOWidget other : wire.getOutputs()) {
                        if (nodes.contains(other.parent)) continue;
                        nodes.add(other.parent);
                        todo.add(other.parent);
                    }
                    wires.addAll(wire.getFullWire());
                }
            }

            if (n.node instanceof FunctionInputsNode f) {
                gatherSnippetFunction(f.function, nodes, todo);
                functions.add(f.function);
            }
            if (n.node instanceof FunctionOutputsNode f) {
                gatherSnippetFunction(f.function, nodes, todo);
                functions.add(f.function);
            }
            if (n.node instanceof FunctionCallNode f) {
                gatherSnippetFunction(f.function, nodes, todo);
                functions.add(f.function);
            }
        }

        List<NodeWidget> nodeList = new ArrayList<>(nodes);
        List<WireWidget> wireList = new ArrayList<>(wires);

        JsonObject json = new JsonObject();
        json.add("nodes", CodeJSON.nodeToJson(nodeList, v -> v.sub(cursor)));
        json.add("wires", CodeJSON.wireToJson(wireList, nodeList::indexOf, v -> v.sub(cursor)));
        json.add("functions", CodeJSON.fnToJson(functions));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gz = new GZIPOutputStream(out);
            gz.write(json.toString().getBytes(StandardCharsets.UTF_8));
            gz.finish();
        } catch (Exception e) {
            FireFlow.LOGGER.error("Error gzipping snippet!", e);
            player.sendMessage(Component.text(Translations.get("error.internal")).color(NamedTextColor.RED));
            return;
        }

        String data = new String(Base64.getEncoder().encode(out.toByteArray()));
        player.sendMessage(Component.text(Translations.get("success.snippet.create", String.valueOf(nodes.size())))
                .clickEvent(ClickEvent.copyToClipboard(data)).color(NamedTextColor.AQUA));
    }

    public void placeSnippet(Player player, byte[] base64Str) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64Str));
            GZIPInputStream gz = new GZIPInputStream(in);
            JsonObject json = JsonParser.parseString(new String(gz.readNBytes(1048576))).getAsJsonObject();

            List<FunctionDefinition> definitions = CodeJSON.fnFromJson(json.getAsJsonArray("functions"));

            Set<String> newFunctionNames = new HashSet<>();
            for (FunctionDefinition fn : definitions) {
                if (functions.containsKey(fn.name) || newFunctionNames.contains(fn.name)) {
                    player.sendMessage(Component.text(Translations.get("error.internal")).color(NamedTextColor.RED));
                    return;
                }
                newFunctionNames.add(fn.name);
            }

            Vec cursor = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(0);
            List<NodeWidget> nodeWidgets = CodeJSON.nodeFromJson(json.getAsJsonArray("nodes"), (id) -> {
                if (functions.containsKey(id)) {
                    return functions.get(id);
                } else {
                    for (FunctionDefinition definition : definitions) {
                        if (definition.name.equals(id)) return definition;
                    }
                }
                return null;
            }, this, v -> v.add(cursor));
            List<WireWidget> wireWidgets = CodeJSON.wireFromJson(json.getAsJsonArray("wires"), nodeWidgets::get, v -> v.add(cursor));

            if (nodeWidgets.contains(null) || wireWidgets.contains(null)) {
                player.sendMessage(Component.text(Translations.get("error.snippet.partial")).color(NamedTextColor.RED));
                nodeWidgets.removeIf(Objects::isNull);
                wireWidgets.removeIf(Objects::isNull);
            }

            rootWidgets.addAll(nodeWidgets);
            rootWidgets.addAll(wireWidgets);
            for (NodeWidget n : nodeWidgets) n.update(space.code);
            for (WireWidget w : wireWidgets) w.update(space.code);
            for (FunctionDefinition fn : definitions) functions.put(fn.name, fn);

            player.sendMessage(Component.text(Translations.get("success.snippet.place", String.valueOf(nodeWidgets.size()))).color(NamedTextColor.AQUA));
        } catch (Exception e) {
            FireFlow.LOGGER.warn("Error reading snippet!", e);
            player.sendMessage(Component.text(Translations.get("error.internal")).color(NamedTextColor.RED));
        }
    }

    private void gatherSnippetFunction(FunctionDefinition fn, Set<NodeWidget> all, Set<NodeWidget> todo) {
        for (Widget w : rootWidgets) {
            if (!(w instanceof NodeWidget other)) continue;
            if (other.node instanceof FunctionOutputsNode otherFn && otherFn.function == fn) {
                if (all.contains(other)) continue;
                all.add(other);
                todo.add(other);
            }
            if (other.node instanceof FunctionInputsNode otherFn && otherFn.function == fn) {
                if (all.contains(other)) continue;
                all.add(other);
                todo.add(other);
            }
            if (other.node instanceof FunctionCallNode otherFn && otherFn.function == fn) {
                if (all.contains(other)) continue;
                all.add(other);
                todo.add(other);
            }
        }
    }

    public List<Widget> getAllWidgetsBetween(Interaction i, Vec p1, Vec p2) {
        List<NodeWidget> nodeWidgets = new ArrayList<>();
        for (Widget w : new HashSet<>(i.editor().rootWidgets)) {
            if (w instanceof NodeWidget nodeWidget) {
                if (isVectorBetween(nodeWidget.getPos(), p1, p2) && isVectorBetween(nodeWidget.getPos().sub(nodeWidget.getSize()), p1, p2))
                    nodeWidgets.add(nodeWidget);
            }
        }

        List<Widget> widgets = new ArrayList<>(nodeWidgets);
        for (NodeWidget w : nodeWidgets) {
            for (NodeIOWidget io : w.getIOWidgets()) {
                for (WireWidget wire : io.connections) {
                    if (widgets.contains(wire)) continue;
                    List<NodeWidget> inputs = wire.getInputs().stream().map(widget -> widget.parent).toList();
                    List<NodeWidget> outputs = wire.getOutputs().stream().map(widget -> widget.parent).toList();
                    if (new HashSet<>(widgets).containsAll(inputs) && new HashSet<>(widgets).containsAll(outputs)) {
                        widgets.addAll(wire.getFullWire());
                    }
                }
            }
        }

        return widgets;
    }

    public static boolean isVectorBetween(Vec v, Vec p1, Vec p2) {
        Vec min = p1.min(p2);
        Vec max = p1.max(p2);

        return min.x() < v.x() && min.y() < v.y()
                && max.x() > v.x() && max.y() > v.y();
    }

    public NodeIOWidget selectIOWidget(Player player) {
        Vec pos = getCursor(player).mul(8).apply(Vec.Operator.CEIL).div(8).withZ(15.999);
        for (Widget w : rootWidgets) {
            if (w.getWidget(pos) instanceof NodeIOWidget io) {
                return io;
            }
        }
        return null;
    }

    public void save() {
        JsonObject data = new JsonObject();

        List<NodeWidget> nodeWidgets = new ArrayList<>();
        for (Widget widget : rootWidgets) {
            if (widget instanceof NodeWidget nodeWidget) {
                nodeWidgets.add(nodeWidget);
            }
        }
        data.add("nodes", CodeJSON.nodeToJson(nodeWidgets, v -> v));

        List<WireWidget> wireWidgets = new ArrayList<>();
        for (Widget widget : rootWidgets) {
            if (widget instanceof WireWidget wireWidget) {
                if (!wireWidget.isValid()) continue;
                wireWidgets.add(wireWidget);
            }
        }
        data.add("wires", CodeJSON.wireToJson(wireWidgets, nodeWidgets::indexOf, v -> v));
        data.add("functions", CodeJSON.fnToJson(this.functions.values()));

        try {
            if (!Files.exists(codePath.getParent())) Files.createDirectories(codePath.getParent());
            Files.writeString(codePath, data.toString());
        } catch (IOException e) {
            FireFlow.LOGGER.error("Failed to save code.json for space " + space.info.id + "!", e);
        }
    }

    public void load() {
        try {
            if (!Files.exists(codePath)) return;
            JsonObject data = JsonParser.parseString(Files.readString(codePath)).getAsJsonObject();

            for (FunctionDefinition fn : CodeJSON.fnFromJson(data.getAsJsonArray("functions"))) {
                functions.put(fn.name, fn);
            }

            List<NodeWidget> nodeWidgets = CodeJSON.nodeFromJson(data.getAsJsonArray("nodes"), functions::get, this, v -> v);
            List<WireWidget> wireWidgets = CodeJSON.wireFromJson(data.getAsJsonArray("wires"), nodeWidgets::get, v -> v);

            if (nodeWidgets.contains(null) || wireWidgets.contains(null)) {
                space.potentiallyBroken = true;
                nodeWidgets.removeIf(Objects::isNull);
                wireWidgets.removeIf(Objects::isNull);
            }

            rootWidgets.addAll(nodeWidgets);
            rootWidgets.addAll(wireWidgets);

            for (NodeWidget n : nodeWidgets) n.update(space.code);
            for (WireWidget w : wireWidgets) w.update(space.code);
        } catch (IOException e) {
            FireFlow.LOGGER.error("Failed to load code.json for space " + space.info.id + "!", e);
        }
    }
}
