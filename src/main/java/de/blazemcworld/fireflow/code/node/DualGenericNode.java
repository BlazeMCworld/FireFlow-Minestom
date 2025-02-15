package de.blazemcworld.fireflow.code.node;

import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.code.type.WireType;
import de.blazemcworld.fireflow.util.Translations;
import net.minestom.server.item.Material;

import java.util.List;

public abstract class DualGenericNode<T, U> extends Node {
    protected final WireType<T> type1;
    protected final WireType<U> type2;

    protected DualGenericNode(String id, Material icon, WireType<T> type1, WireType<U> type2) {
        super(id, icon);
        this.type1 = type1;
        this.type2 = type2;
    }

    @Override
    public boolean acceptsType(WireType<?> type, int index) {
        return AllTypes.isValue(type);
    }

    @Override
    public List<WireType<?>> getTypes() {
        return List.of(type1, type2);
    }

    @Override
    public String getTitle() {
        if (type1 == null || type2 == null) return Translations.get("node." + id + ".base_title");
        return Translations.get("node." + id + ".title", type1.getName(), type2.getName());
    }

    @Override
    public int getTypeCount() {
        return 2;
    }

    @Override
    public Node copyWithTypes(List<WireType<?>> types) {
        return copyWithType(types.get(0), types.get(1));
    }

    public abstract Node copyWithType(WireType<?> type1, WireType<?> type2);
}

