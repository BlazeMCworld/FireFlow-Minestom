package de.blazemcworld.fireflow.code.node.impl.world;

import de.blazemcworld.fireflow.code.CodeThread;
import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.type.VectorType;
import de.blazemcworld.fireflow.util.Config;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

public class SetRegionNode extends Node {
    public SetRegionNode() {
        super("set_region", Material.POLISHED_ANDESITE);
        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<Vec> corner1 = new Input<>("corner1", VectorType.INSTANCE);
        Input<Vec> corner2 = new Input<>("corner2", VectorType.INSTANCE);
        Input<String> block = new Input<>("block", StringType.INSTANCE);
        Output<Void> now = new Output<>("now", SignalType.INSTANCE);
        Output<Void> then = new Output<>("then", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            Block placedBlock = Block.fromKey(block.getValue(ctx));
            if (placedBlock != null) {
                Vec corner1Value = corner1.getValue(ctx);
                Vec corner2Value = corner2.getValue(ctx);

                int maxDistance = Config.store.limits().spaceChunkDistance() * 16;
                Vec min = corner1Value.min(corner2Value).max(-maxDistance, -64, -maxDistance);
                Vec max = corner1Value.max(corner2Value).min(maxDistance - 1, 319, maxDistance - 1);
                int[] chunk = { min.chunkX(), min.chunkZ() };

                int yStart = min.blockY();
                int yEnd = max.blockY() + 1;
                CodeThread worker = ctx.subThread();

                Runnable[] step = { null };
                step[0] = () -> {
                    if (ctx.evaluator.isStopped()) return;

                    if (ctx.evaluator.space.play.getChunk(chunk[0], chunk[1]) == null) {
                        ctx.evaluator.space.play.loadChunk(chunk[0], chunk[1]).thenRun(() -> {
                            worker.submit(step[0]);
                            worker.resume();
                        });
                        return;
                    }

                    int xStart = Math.max(chunk[0] * 16, min.blockX());
                    int xEnd = Math.min(chunk[0] * 16 + 16, max.blockX() + 1);
                    int zStart = Math.max(chunk[1] * 16, min.blockZ());
                    int zEnd = Math.min(chunk[1] * 16 + 16, max.blockZ() + 1);

                    AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
                    for (int x = xStart; x < xEnd; x++) {
                        for (int z = zStart; z < zEnd; z++) {
                            for (int y = yStart; y < yEnd; y++) {
                                batch.setBlock(x, y, z, placedBlock);
                            }
                        }
                    }

                    batch.apply(ctx.evaluator.space.play, () -> {
                        if (ctx.evaluator.isStopped()) return;
                        chunk[0]++;
                        if (chunk[0] > max.chunkX()) {
                            chunk[0] = min.chunkX();
                            chunk[1]++;
                            if (chunk[1] > max.chunkZ()) {
                                worker.sendSignal(then);
                                worker.resume();
                                return;
                            }
                        }

                        worker.submit(step[0]);
                        worker.resume();
                    });
                };

                worker.submit(step[0]);
                worker.clearQueue();
            }
            ctx.sendSignal(now);
        });
    }

    @Override
    public Node copy() {
        return new SetRegionNode();
    }
}