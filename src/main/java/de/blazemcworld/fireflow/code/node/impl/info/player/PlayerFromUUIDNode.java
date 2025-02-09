package de.blazemcworld.fireflow.code.node.impl.info.player;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.item.Material;

import java.util.UUID;

public class PlayerFromUUIDNode extends Node {
    public PlayerFromUUIDNode() {
        super("player_from_uuid", Material.SKELETON_SKULL);

        Input<String> uuid = new Input<>("uuid", StringType.INSTANCE);
        Output<PlayerValue> player = new Output<>("player", PlayerType.INSTANCE);

        player.valueFrom(ctx -> {
            try {
                return new PlayerValue(UUID.fromString(uuid.getValue(ctx)));
            } catch (IllegalArgumentException e) {
                return PlayerType.INSTANCE.defaultValue();
            }
        });
    }

    @Override
    public Node copy() {
        return new PlayerFromUUIDNode();
    }
}