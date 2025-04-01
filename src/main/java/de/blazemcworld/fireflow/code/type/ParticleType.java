package de.blazemcworld.fireflow.code.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.particle.Particle;

import java.util.Base64;

public class ParticleType extends WireType<Particle> {

    public static final ParticleType INSTANCE = new ParticleType();

    private ParticleType() {
        super("particle", NamedTextColor.DARK_AQUA, Material.GLOWSTONE_DUST);
    }

    @Override
    public Particle defaultValue() {
        return Particle.DUST;
    }

    @Override
    public Particle checkType(Object obj) {
        if (obj instanceof Particle p) return p;
        return null;
    }

    @Override
    public Particle parseInset(String str) {
        return Particle.fromKey(str);
    }

    @Override
    public JsonElement toJson(Particle p) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(Particle.NETWORK_TYPE, p);
        return new JsonPrimitive(Base64.getEncoder().encodeToString(buffer.read(NetworkBuffer.RAW_BYTES)));
    }

    @Override
    public Particle fromJson(JsonElement json) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(NetworkBuffer.RAW_BYTES, Base64.getDecoder().decode(json.getAsString()));
        return buffer.read(Particle.NETWORK_TYPE);
    }

    @Override
    public boolean valuesEqual(Particle a, Particle b) {
        return a.equals(b);
    }

    @Override
    protected String stringifyInternal(Particle value) {
        return value.key().value();
    }
}
