package de.blazemcworld.fireflow.code.node;

import de.blazemcworld.fireflow.FireFlow;
import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionCallNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionInputsNode;
import de.blazemcworld.fireflow.code.node.impl.function.FunctionOutputsNode;
import de.blazemcworld.fireflow.code.type.ListType;
import de.blazemcworld.fireflow.code.type.WireType;
import de.blazemcworld.fireflow.code.value.ListValue;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Node {

    public final String id;
    public final Material icon;
    public List<Input<?>> inputs = new ArrayList<>();
    public List<Varargs<?>> varargs = new ArrayList<>();
    public List<Output<?>> outputs = new ArrayList<>();
    public Node clonedFrom;
    private static final Pattern descriptionReferencePattern = Pattern.compile("(in|out)#(\\w+)");

    protected Node(String id, Material icon) {
        this.id = id;
        this.icon = icon;
    }

    public String getTitle() {
        return Translations.get("node." + id + ".title");
    }

    public abstract Node copy();

    public void init(CodeEvaluator evaluator) {
    }

    public List<WireType<?>> getTypes() {
        return null;
    }

    public int getTypeCount() {
        return 0;
    }

    public boolean acceptsType(WireType<?> type, int index) {
        return false;
    }

    public Node copyWithTypes(List<WireType<?>> types) {
        return copy();
    }

    public Component getIngameDescription() {
        Component out = Component.empty();
        String raw = Translations.get("node." + id + ".description");
        Matcher m = descriptionReferencePattern.matcher(raw);
        int index = 0;
        while (m.find()) {
            out = out.append(Component.text(raw.substring(index, m.start())));
            index = m.end();
            if (m.group(1).equals("in")) {
                out = out.append(Component.text(Translations.get("node." + id + ".input." + m.group(2))).color(NamedTextColor.YELLOW));
            } else {
                out = out.append(Component.text(Translations.get("node." + id + ".output." + m.group(2))).color(NamedTextColor.YELLOW));
            }
        }
        out = out.append(Component.text(raw.substring(index)));
        return out;
    }

    public String getWikiDescription() {
        StringBuilder out = new StringBuilder();
        String raw = Translations.get("node." + id + ".description");
        Matcher m = descriptionReferencePattern.matcher(raw);
        int index = 0;
        while (m.find()) {
            out.append(raw, index, m.start());
            index = m.end();
            if (m.group(1).equals("in")) {
                out.append("`").append(Translations.get("node." + id + ".input." + m.group(2))).append("`");
            } else {
                out.append("`").append(Translations.get("node." + id + ".output." + m.group(2))).append("`");
            }
        }
        out.append(raw.substring(index));
        return out.toString();
    }

    public class Input<T> {
        public final String id;
        public final WireType<T> type;
        public String inset;
        public Output<?> connected;
        public Varargs<T> varargsParent;
        private Consumer<CodeThread> logic;
        public List<String> options;

        public Input(String id, WireType<T> type) {
            this.id = id;
            this.type = type;
            inputs.add(this);
        }

        @SuppressWarnings("unchecked")
        public T getValue(CodeThread ctx) {
            if (connected != null) {
                Object out = connected.computeNow(ctx);
                if (connected.type == type) return (T) out;
                return type.convert(connected.type, out);
            }
            if (inset != null) return type.parseInset(inset);
            return type.defaultValue();
        }

        public Input<T> options(String... options) {
            this.options = Arrays.asList(options);
            setInset(options[0]);
            return this;
        }

        public void onSignal(Consumer<CodeThread> logic) {
            this.logic = logic;
        }

        private void computeNow(CodeThread ctx) {
            if (logic == null) return;
            logic.accept(ctx);
        }

        public String getName() {
            if (Node.this instanceof FunctionCallNode || Node.this instanceof FunctionOutputsNode || Node.this instanceof FunctionInputsNode) return id;
            if (varargsParent != null) return Translations.get("node." + Node.this.id + ".input." + varargsParent.id);
            return Translations.get("node." + Node.this.id + ".input." + id);
        }

        public Node getNode() {
            return Node.this;
        }

        public void setInset(String value) {
            inset = value;
            connected = null;
            if (varargsParent != null) varargsParent.update();
            if (inset == null && options != null) inset = options.getFirst();
        }

        public void connect(Output<T> output) {
            if (output == null) {
                connected = null;
            } else if (canUnderstand(output.type)) {
                connected = output;
            } else {
                FireFlow.LOGGER.warn("Called input.connect() with invalid wire type!");
            }
            inset = null;
            if (varargsParent != null) varargsParent.update();
            if (connected == null && options != null) inset = options.getFirst();
        }

        public boolean canUnderstand(WireType<?> other) {
            if (other == type || type.canConvert(other)) return true;

            if (varargsParent != null && other instanceof ListType<?> l) {
                return l.elementType == type || type.canConvert(l.elementType);
            }
            return false;
        }
    }

    public class Output<T> {
        public final String id;
        public final WireType<T> type;
        public Input<T> connected;
        private Function<CodeThread, T> logic;

        public Output(String id, WireType<T> type) {
            this.id = id;
            this.type = type;
            outputs.add(this);
        }

        public void valueFrom(Function<CodeThread, T> logic) {
            this.logic = logic;
        }

        public void sendSignalImmediately(CodeThread ctx) {
            if (connected == null) return;
            connected.computeNow(ctx);
        }

        public T computeNow(CodeThread ctx) {
            return logic.apply(ctx);
        }

        public String getName() {
            if (Node.this instanceof FunctionCallNode || Node.this instanceof FunctionOutputsNode || Node.this instanceof FunctionInputsNode) return id;
            return Translations.get("node." + Node.this.id + ".output." + id);
        }

        public Node getNode() {
            return Node.this;
        }

        public void valueFromThread() {
            logic = (ctx) -> ctx.getThreadValue(this);
        }
    }

    public class Varargs<T> {
        public final String id;
        public final WireType<T> type;
        public List<Input<T>> children = new ArrayList<>();
        public boolean ignoreUpdates = false;

        public Varargs(String id, WireType<T> type) {
            this.id = id;
            this.type = type;
            varargs.add(this);
            addInput(UUID.randomUUID().toString());
        }

        @SuppressWarnings("unchecked")
        public List<T> getVarargs(CodeThread ctx) {
            List<T> list = new ArrayList<>();
            for (Input<T> input : children) {
                if (input.inset == null && input.connected == null) continue;
                if (input.connected != null && input.type != input.connected.type && input.connected.type instanceof ListType<?> l && input.type.canConvert(l.elementType)) {
                    ListValue<?> spread = (ListValue<?>) input.connected.computeNow(ctx);
                    for (int i = 0; i < spread.size(); i++) {
                        if (spread.type == input.type) {
                            list.add((T) spread.get(i));
                        } else {
                            list.add(input.type.convert(spread.type, spread.get(i)));
                        }
                    }
                    continue;
                }
                list.add(input.getValue(ctx));
            }
            return list;
        }

        public void update() {
            if (ignoreUpdates) return;
            List<Input<T>> used = new ArrayList<>();
            for (Input<T> input : children) {
                if (input.inset != null || input.connected != null) {
                    used.add(input);
                }
            }

            if (used.size() == children.size()) {
                addInput(UUID.randomUUID().toString());
                return;
            }

            for (Input<T> input : new ArrayList<>(children)) {
                if (used.contains(input)) continue;
                if (input != children.getLast()) {
                    inputs.remove(input);
                    children.remove(input);
                }
            }

            if (!used.contains(children.getLast())) return;
            addInput(UUID.randomUUID().toString());
        }

        public void addInput(String uuid) {
            Input<T> input = new Input<>(uuid, type);
            input.varargsParent = this;
            children.add(input);
            inputs.remove(input);
            for (int i = inputs.size() - 1; i >= 0; i--) {
                if (inputs.get(i).varargsParent == this) {
                    inputs.add(i + 1, input);
                    return;
                }
            }
            inputs.add(input);
        }
    }
}
