package de.blazemcworld.fireflow.code.node.impl.dictionary;

import de.blazemcworld.fireflow.code.node.DualGenericNode;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.DictionaryType;
import de.blazemcworld.fireflow.code.type.ListType;
import de.blazemcworld.fireflow.code.type.WireType;
import de.blazemcworld.fireflow.code.value.DictionaryValue;
import de.blazemcworld.fireflow.code.value.ListValue;
import net.minestom.server.item.Material;

public class DictionaryKeysNode<K, V> extends DualGenericNode<K, V> {

    public DictionaryKeysNode(WireType<K> type1, WireType<V> type2) {
        super("dictionary_keys", Material.HOPPER, type1, type2);

        Input<DictionaryValue<K, V>> dict = new Input<>("dictionary", DictionaryType.of(type1, type2));
        Output<ListValue<K>> keys = new Output<>("keys", ListType.of(type1));

        keys.valueFrom((ctx) -> new ListValue<>(type1, dict.getValue(ctx).keys()));
    }

    @Override
    public Node copy() {
        return new DictionaryKeysNode<>(type1, type2);
    }

    @Override
    public Node copyWithType(WireType<?> type1, WireType<?> type2) {
        return new DictionaryKeysNode<>(type1, type2);
    }
}


