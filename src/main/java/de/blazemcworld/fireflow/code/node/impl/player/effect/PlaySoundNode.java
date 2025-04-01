package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.*;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

public class PlaySoundNode extends Node {

    public PlaySoundNode() {
        super("play_sound", Material.NOTE_BLOCK);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<String> sound = new Input<>("sound", StringType.INSTANCE);
        Input<String> mode = new Input<>("mode", StringType.INSTANCE);
        Input<Double> volume = new Input<>("volume", NumberType.INSTANCE);
        Input<Double> pitch = new Input<>("pitch", NumberType.INSTANCE);
        Input<Vec> position = new Input<>("position", VectorType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            player.getValue(ctx).tryUse(ctx, p -> {
                p.playSound(Sound.sound(
                        Key.key(sound.getValue(ctx), ':'), Sound.Source.NAMES.valueOr(mode.getValue(ctx).toUpperCase(), Sound.Source.MASTER),
                        volume.getValue(ctx).floatValue(), pitch.getValue(ctx).floatValue()
                ), position.getValue(ctx));
            });
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new PlaySoundNode();
    }
}

