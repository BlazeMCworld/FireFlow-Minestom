package de.blazemcworld.fireflow.code;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.node.NodeList;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionCallNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionDefinition;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionInputsNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionOutputsNode;
import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.code.type.WireType;
import de.blazemcworld.fireflow.code.widget.NodeIOWidget;
import de.blazemcworld.fireflow.code.widget.NodeWidget;
import de.blazemcworld.fireflow.code.widget.WireWidget;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

import java.util.*;
import java.util.function.Function;

public class CodeJSON {

    public static JsonArray nodeToJson(Collection<NodeWidget> nodeWidgets, Function<Vec, Vec> transformPos) {
        JsonArray nodes = new JsonArray();

        for (NodeWidget nodeWidget : nodeWidgets) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", nodeWidget.node.id);
            Vec transformed = transformPos.apply(nodeWidget.getPos());
            entry.addProperty("x", transformed.x());
            entry.addProperty("y", transformed.y());

            if (nodeWidget.node.getTypeCount() > 0) {
                JsonArray types = new JsonArray();
                for (WireType<?> type : nodeWidget.node.getTypes()) {
                    types.add(AllTypes.toJson(type));
                }
                entry.add("types", types);
            }

            JsonObject insets = new JsonObject();
            for (NodeIOWidget io : nodeWidget.getIOWidgets()) {
                if (io.isInput() && io.input.inset != null) {
                    insets.addProperty(io.input.id, io.input.inset);
                }
            }
            if (!insets.isEmpty()) {
                entry.add("insets", insets);
            }

            JsonObject inputs = new JsonObject();
            JsonObject outputs = new JsonObject();
            for (NodeIOWidget io : nodeWidget.getIOWidgets()) {
                if (io.isInput() && io.input.connected != null) {
                    int nodeIndex = 0;
                    for (NodeWidget node : nodeWidgets) {
                        if (node.node != io.input.connected.getNode()) {
                            nodeIndex++;
                            continue;
                        }
                        int outputIndex = node.node.outputs.indexOf(io.input.connected);
                        inputs.addProperty(io.input.id, nodeIndex + ":" + outputIndex);
                        break;
                    }
                } else if (!io.isInput() && io.output.connected != null) {
                    int nodeIndex = 0;
                    for (NodeWidget node : nodeWidgets) {
                        if (node.node != io.output.connected.getNode()) {
                            nodeIndex++;
                            continue;
                        }
                        int inputIndex = node.node.inputs.indexOf(io.output.connected);
                        outputs.addProperty(io.output.id, nodeIndex + ":" + inputIndex);
                        break;
                    }
                }
            }
            if (!inputs.isEmpty()) {
                entry.add("inputs", inputs);
            }
            if (!outputs.isEmpty()) {
                entry.add("outputs", outputs);
            }

            if (!nodeWidget.node.varargs.isEmpty()) {
                JsonObject varargsObj = new JsonObject();
                for (Node.Varargs<?> varargs : nodeWidget.node.varargs) {
                    JsonArray varargsEntry = new JsonArray();
                    for (Node.Input<?> input : varargs.children) {
                        if (input.inset == null && input.connected == null) continue;
                        varargsEntry.add(input.id);
                    }
                    varargsObj.add(varargs.id, varargsEntry);
                }
                entry.add("varargs", varargsObj);
            }

            switch (nodeWidget.node) {
                case FunctionInputsNode inputsNode -> entry.addProperty("function", inputsNode.function.name);
                case FunctionOutputsNode outputsNode -> entry.addProperty("function", outputsNode.function.name);
                case FunctionCallNode callNode -> entry.addProperty("function", callNode.function.name);
                default -> {
                }
            }

            nodes.add(entry);
        }

        return nodes;
    }

    public static JsonArray wireToJson(List<WireWidget> wireWidgets, Function<NodeWidget, Integer> node2id,  Function<Vec, Vec> transformPos) {
        JsonArray wires = new JsonArray();

        for (WireWidget wireWidget : wireWidgets) {
            JsonObject wireObj = new JsonObject();
            wireObj.add("type", AllTypes.toJson(wireWidget.type()));
            Vec transformedFrom = transformPos.apply(wireWidget.line.from);
            Vec transformedTo = transformPos.apply(wireWidget.line.to);
            wireObj.addProperty("fromX", transformedFrom.x());
            wireObj.addProperty("fromY", transformedFrom.y());
            wireObj.addProperty("toX", transformedTo.x());
            wireObj.addProperty("toY", transformedTo.y());

            if (wireWidget.previousOutput != null) {
                wireObj.addProperty("previousOutputNode", node2id.apply(wireWidget.previousOutput.parent));
                wireObj.addProperty("previousOutputId", wireWidget.previousOutput.output.id);
            }

            JsonArray previousWires = new JsonArray();
            for (WireWidget previousWire : wireWidget.previousWires) {
                previousWires.add(wireWidgets.indexOf(previousWire));
            }
            if (!previousWires.isEmpty()) {
                wireObj.add("previousWires", previousWires);
            }

            if (wireWidget.nextInput != null) {
                wireObj.addProperty("nextInputNode", node2id.apply(wireWidget.nextInput.parent));
                wireObj.addProperty("nextInputId", wireWidget.nextInput.input.id);
            }

            JsonArray nextWires = new JsonArray();
            for (WireWidget nextWire : wireWidget.nextWires) {
                nextWires.add(wireWidgets.indexOf(nextWire));
            }
            if (!nextWires.isEmpty()) {
                wireObj.add("nextWires", nextWires);
            }

            wires.add(wireObj);
        }

        return wires;
    }

    public static JsonArray fnToJson(Collection<FunctionDefinition> functions) {
        JsonArray out = new JsonArray();

        for (FunctionDefinition function : functions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", function.name);
            obj.addProperty("icon", function.icon.key().value());
            JsonArray inputs = new JsonArray();
            for (Node.Output<?> input : function.inputsNode.outputs) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("name", input.id);
                inputObj.add("type", AllTypes.toJson(input.type));
                inputs.add(inputObj);
            }
            obj.add("inputs", inputs);
            JsonArray outputs = new JsonArray();
            for (Node.Input<?> output : function.outputsNode.inputs) {
                JsonObject outputObj = new JsonObject();
                outputObj.addProperty("name", output.id);
                outputObj.add("type", AllTypes.toJson(output.type));
                outputs.add(outputObj);
            }
            obj.add("outputs", outputs);
            out.add(obj);
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    public static List<NodeWidget> nodeFromJson(JsonArray json, Function<String, FunctionDefinition> id2Fn, CodeEditor editor, Function<Vec, Vec> transformPos) {
        List<NodeWidget> out = new ArrayList<>();
        List<Runnable> todo = new ArrayList<>();

        for (JsonElement nodeElem : json) {
            JsonObject nodeObj = nodeElem.getAsJsonObject();
            String type = nodeObj.get("type").getAsString();
            double x = nodeObj.get("x").getAsDouble();
            double y = nodeObj.get("y").getAsDouble();

            List<WireType<?>> nodeTypes = new ArrayList<>();
            if (nodeObj.has("types")) {
                JsonArray types = nodeObj.getAsJsonArray("types");
                for (JsonElement str : types) {
                    nodeTypes.add(AllTypes.fromJson(str));
                }
            }

            Node node = null;
            for (Node n : NodeList.root.collectNodes()) {
                if (n.id.equals(type)) {
                    if (nodeTypes.isEmpty()) {
                        node = n.copy();
                    } else {
                        node = n.copyWithTypes(nodeTypes);
                    }
                    break;
                }
            }

            if (node == null && nodeObj.has("function")) {
                FunctionDefinition function = id2Fn.apply(nodeObj.get("function").getAsString());
                if (function != null) {
                    node = switch (type) {
                        case "function_inputs" -> function.inputsNode;
                        case "function_outputs" -> function.outputsNode;
                        case "function_call" -> new FunctionCallNode(function);
                        default -> null;
                    };
                }
            }

            if (node == null) {
                out.add(null);
                continue;
            }

            if (nodeObj.has("varargs")) {
                JsonObject varargsObj = nodeObj.getAsJsonObject("varargs");
                for (Map.Entry<String, JsonElement> entry : varargsObj.entrySet()) {
                    String varargsId = entry.getKey();
                    JsonArray children = entry.getValue().getAsJsonArray();
                    for (Node.Varargs<?> varargs : node.varargs) {
                        varargs.ignoreUpdates = true;
                        node.inputs.removeAll(varargs.children);
                        varargs.children.clear();
                        if (varargs.id.equals(varargsId)) {
                            for (JsonElement child : children) {
                                varargs.addInput(child.getAsString());
                            }
                            break;
                        }
                    }
                }
            }

            NodeWidget nodeWidget = new NodeWidget(node, editor);
            nodeWidget.setPos(transformPos.apply(new Vec(x, y, 15.999)));
            out.add(nodeWidget);

            if (nodeObj.has("insets")) {
                JsonObject insets = nodeObj.getAsJsonObject("insets");
                for (Map.Entry<String, JsonElement> entry : insets.entrySet()) {
                    String inputId = entry.getKey();
                    String inset = entry.getValue().getAsString();

                    for (NodeIOWidget io : nodeWidget.getIOWidgets()) {
                        if (io.isInput() && io.input.id.equals(inputId)) {
                            io.insetValue(inset, editor);
                            break;
                        }
                    }
                }
            }

            todo.add(() -> {
                if (nodeObj.has("inputs")) {
                    JsonObject inputs = nodeObj.getAsJsonObject("inputs");
                    for (Map.Entry<String, JsonElement> entry : inputs.entrySet()) {
                        String inputId = entry.getKey();
                        int nodeIndex = Integer.parseInt(entry.getValue().getAsString().split(":")[0]);
                        int outputId = Integer.parseInt(entry.getValue().getAsString().split(":")[1]);

                        NodeWidget other = out.get(nodeIndex);
                        if (other == null) continue;
                        for (Node.Input<?> input : nodeWidget.node.inputs) {
                            if (input.id.equals(inputId)) {
                                ((Node.Input<Object>) input).connect((Node.Output<Object>) other.node.outputs.get(outputId));
                                break;
                            }
                        }
                    }
                }
                if (nodeObj.has("outputs")) {
                    JsonObject outputs = nodeObj.getAsJsonObject("outputs");
                    for (Map.Entry<String, JsonElement> entry : outputs.entrySet()) {
                        String outputId = entry.getKey();
                        int nodeIndex = Integer.parseInt(entry.getValue().getAsString().split(":")[0]);
                        int inputId = Integer.parseInt(entry.getValue().getAsString().split(":")[1]);

                        NodeWidget other = out.get(nodeIndex);
                        if (other == null) continue;
                        for (Node.Output<?> output : nodeWidget.node.outputs) {
                            if (output.id.equals(outputId)) {
                                ((Node.Output<Object>) output).connected = (Node.Input<Object>) other.node.inputs.get(inputId);
                                break;
                            }
                        }
                    }
                }

                for (Node.Varargs<?> varargs : nodeWidget.node.varargs) {
                    varargs.ignoreUpdates = false;
                    varargs.update();
                }
            });
        }

        for (Runnable item : todo) item.run();
        return out;
    }

    public static List<WireWidget> wireFromJson(JsonArray wires, Function<Integer, NodeWidget> id2Node, Function<Vec, Vec> transformPos) {
        List<WireWidget> wireWidgets = new ArrayList<>();
        List<Runnable> todo = new ArrayList<>();

        for (JsonElement wireElem : wires) {
            JsonObject wireObj = wireElem.getAsJsonObject();
            JsonElement type = wireObj.get("type");
            double fromX = wireObj.get("fromX").getAsDouble();
            double fromY = wireObj.get("fromY").getAsDouble();
            double toX = wireObj.get("toX").getAsDouble();
            double toY = wireObj.get("toY").getAsDouble();

            WireType<?> typeInst = AllTypes.fromJson(type);
            WireWidget wire = new WireWidget(typeInst, transformPos.apply(new Vec(fromX, fromY, 15.999)), transformPos.apply(new Vec(toX, toY, 15.999)));
            if (wireObj.has("previousOutputNode") && wireObj.has("previousOutputId")) {
                int nodeIndex = wireObj.get("previousOutputNode").getAsInt();
                String outputId = wireObj.get("previousOutputId").getAsString();
                NodeWidget node = id2Node.apply(nodeIndex);
                if (node == null) {
                    wireWidgets.add(null);
                    continue;
                }
                for (NodeIOWidget io : node.getIOWidgets()) {
                    if (!io.isInput() && io.output.id.equals(outputId)) {
                        wire.setPreviousOutput(io);
                        io.connections.add(wire);
                        break;
                    }
                }
            }

            if (wireObj.has("nextInputId") && wireObj.has("nextInputNode")) {
                int nodeIndex = wireObj.get("nextInputNode").getAsInt();
                String inputId = wireObj.get("nextInputId").getAsString();
                NodeWidget node = id2Node.apply(nodeIndex);
                if (node == null) {
                    wireWidgets.add(null);
                    continue;
                }
                for (NodeIOWidget io : node.getIOWidgets()) {
                    if (io.isInput() && io.input.id.equals(inputId)) {
                        wire.setNextInput(io);
                        io.connections.add(wire);
                        break;
                    }
                }
            }

            todo.add(() -> {
                if (wireObj.has("previousWires")) {
                    for (JsonElement previousWireElem : wireObj.getAsJsonArray("previousWires")) {
                        int index = previousWireElem.getAsInt();
                        if (wireWidgets.get(index) == null) continue;
                        wire.previousWires.add(wireWidgets.get(index));
                    }
                }
                if (wireObj.has("nextWires")) {
                    for (JsonElement nextWireElem : wireObj.getAsJsonArray("nextWires")) {
                        int index = nextWireElem.getAsInt();
                        if (wireWidgets.get(index) == null) continue;
                        wire.nextWires.add(wireWidgets.get(index));
                    }
                }
            });

            wireWidgets.add(wire);
        }

        for (Runnable item : todo) item.run();
        for (WireWidget w : wireWidgets) {
            if (w == null) continue;
            if (w.previousOutput == null && w.previousWires.isEmpty()) {
                removeInvalid(w, wireWidgets);
                continue;
            }
            if (w.nextInput == null && w.nextWires.isEmpty()) {
                removeInvalid(w, wireWidgets);
                continue;
            }
        }
        return wireWidgets;
    }

    private static void removeInvalid(WireWidget w, List<WireWidget> all) {
        int index = all.indexOf(w);
        if (index >= 0) {
            all.set(index, null);
        }

        for (WireWidget other : w.previousWires) {
            other.nextWires.remove(w);
            if (other.previousOutput == null && other.previousWires.isEmpty()) {
                removeInvalid(other, all);
                continue;
            }
            if (other.nextInput == null && other.nextWires.isEmpty()) {
                removeInvalid(other, all);
                continue;
            }
        }
        for (WireWidget other : w.nextWires) {
            other.previousWires.remove(w);
            if (other.previousOutput == null && other.previousWires.isEmpty()) {
                removeInvalid(other, all);
                continue;
            }
            if (other.nextInput == null && other.nextWires.isEmpty()) {
                removeInvalid(other, all);
                continue;
            }
        }
    }

    public static List<FunctionDefinition> fnFromJson(JsonArray json) {
        List<FunctionDefinition> out = new ArrayList<>();
        for (JsonElement function : json) {
            JsonObject obj = function.getAsJsonObject();
            String name = obj.get("name").getAsString();
            Material icon = obj.has("icon") ? Material.fromKey(obj.get("icon").getAsString()) : null;
            if (icon == null) icon = Material.COMMAND_BLOCK;
            FunctionDefinition functionDefinition = new FunctionDefinition(name, icon);

            for (JsonElement input : obj.getAsJsonArray("inputs")) {
                JsonObject inputObj = input.getAsJsonObject();
                String inputName = inputObj.get("name").getAsString();
                functionDefinition.addInput(inputName, AllTypes.fromJson(inputObj.get("type")));
            }
            for (JsonElement output : obj.getAsJsonArray("outputs")) {
                JsonObject outputObj = output.getAsJsonObject();
                String outputName = outputObj.get("name").getAsString();
                functionDefinition.addOutput(outputName, AllTypes.fromJson(outputObj.get("type")));
            }
            out.add(functionDefinition);
        }
        return out;
    }
}
