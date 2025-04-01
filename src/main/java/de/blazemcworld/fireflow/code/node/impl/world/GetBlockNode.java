package de.blazemcworld.fireflow.code.node.impl.world;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.type.VectorType;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.item.Material;

public class GetBlockNode extends Node {
    public GetBlockNode() {
        super("get_block", Material.ENDER_EYE);

        Input<Vec> position = new Input<>("position", VectorType.INSTANCE);
        Output<String> block = new Output<>("block", StringType.INSTANCE);

        block.valueFrom((ctx) -> {
            Vec pos = position.getValue(ctx);
            Chunk c = ctx.evaluator.space.play.getChunkAt(pos);
            if (c == null) return "";
            return c.getBlock(pos.sub(c.toPosition())).key().value();
        });
    }

    @Override
    public Node copy() {
        return new GetBlockNode();
    }
}