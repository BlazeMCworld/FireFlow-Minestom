package de.blazemcworld.fireflow.space;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.blazemcworld.fireflow.FireFlow;
import de.blazemcworld.fireflow.code.CodeDebugger;
import de.blazemcworld.fireflow.code.CodeEditor;
import de.blazemcworld.fireflow.code.CodeEvaluator;
import de.blazemcworld.fireflow.code.VariableStore;
import de.blazemcworld.fireflow.util.ChunkLoadingBlockBatch;
import de.blazemcworld.fireflow.util.SpaceInstance;
import de.blazemcworld.fireflow.util.Transfer;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class Space {

    public final SpaceInfo info;
    public final InstanceContainer play = new SpaceInstance();
    public final InstanceContainer code = MinecraftServer.getInstanceManager().createInstanceContainer();
    public final CodeEditor editor;
    public CodeEvaluator evaluator;
    public CodeDebugger debugger;
    public ChunkLoadingBlockBatch spaceBlockBatch;
    private long emptySince = -1;
    private boolean loaded = true;
    public final VariableStore savedVariables = new VariableStore();
    public boolean potentiallyBroken = false;

    public Space(SpaceInfo info) {
        this.info = info;

        play.setTimeRate(0);
        play.setChunkSupplier(LightingChunk::new);
        play.setChunkLoader(new AnvilLoader("spaces/" + info.id + "/world"));

        code.setTimeRate(0);
        code.setChunkSupplier(LightingChunk::new);
        code.setChunkLoader(IChunkLoader.noop());

        play.setGenerator((unit) -> {
            if (Math.abs(unit.absoluteStart().x() + 8) > 16) return;
            if (Math.abs(unit.absoluteStart().z() + 8) > 16) return;
            unit.modifier().fillHeight(-1, 0, Block.SMOOTH_STONE);
        });

        code.setGenerator(unit -> {
            if (unit.absoluteStart().z() != 16.0) return;
            unit.modifier().fill(
                    new BlockVec(0, 0, 0).add(unit.absoluteStart()),
                    new BlockVec(16, 128, 1).add(unit.absoluteStart()),
                    Block.POLISHED_BLACKSTONE
            );
        });

        editor = new CodeEditor(this);
        debugger = new CodeDebugger(this);
        evaluator = new CodeEvaluator(this);

        play.eventNode().addListener(PlayerSpawnEvent.class, event -> {
            emptySince = -1;
            if (potentiallyBroken) event.getPlayer().sendMessage(Component.text(Translations.get("error.space.potentially_broken")).color(NamedTextColor.RED));
        });
        code.eventNode().addListener(PlayerSpawnEvent.class, event -> {
            emptySince = -1;
            if (potentiallyBroken) event.getPlayer().sendMessage(Component.text(Translations.get("error.space.potentially_broken")).color(NamedTextColor.RED));
        });
        
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            if (!loaded) return TaskSchedule.stop();
            if (emptySince == -1 && play.getPlayers().isEmpty() && code.getPlayers().isEmpty()) {
                emptySince = System.currentTimeMillis();
            }
            return TaskSchedule.seconds(1);
        }, TaskSchedule.seconds(1));

        Path path = Path.of("spaces/" + info.id + "/variables.json");
        if (Files.exists(path)) {
            try {
                savedVariables.load(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            } catch (IOException e) {
                FireFlow.LOGGER.error("Failed to load variables.json!", e);
            }
        }
    }

    public boolean isInactive() {
        return emptySince != -1 && System.currentTimeMillis() - emptySince > 10000;
    }

    public void reload(String reason) {
        evaluator.stop();
        for (Player player : play.getPlayers()) {
            player.sendMessage(Component.text(Translations.get("reload." + reason)).color(reason.equals("cpu") ? NamedTextColor.RED : NamedTextColor.YELLOW));
            if (isOwnerOrContributor(player)) {
                Transfer.move(player, code);
            } else {
                Transfer.move(player, Lobby.instance);
            }
        }
        if (reason.equals("clear_world")) {
            for (Chunk c : List.copyOf(play.getChunks())) {
                play.unloadChunk(c);
            }
            try {
                deleteRecursively(Path.of("spaces/" + info.id + "/world"));
            } catch (IOException e) {
                FireFlow.LOGGER.error("Failed to delete spaces/" + info.id + "/world!", e);
            }
        } else {
            save();
        }
        evaluator = new CodeEvaluator(this);
    }

    public void save() {
        if (potentiallyBroken) {
            for (Chunk c : List.copyOf(play.getChunks())) {
                if (c.getViewers().isEmpty()) {
                    play.unloadChunk(c);
                }
            }
            return;
        }

        play.saveChunksToStorage().thenAccept((v) -> {
            for (Chunk c : List.copyOf(play.getChunks())) {
                if (c.getViewers().isEmpty()) {
                    play.unloadChunk(c);
                }
            }
        });
        editor.save();

        JsonObject vars = savedVariables.toJson();
        try {
            Files.writeString(Path.of("spaces/" + info.id + "/variables.json"), vars.toString());
        } catch (IOException e) {
            FireFlow.LOGGER.error("Failed to save variables.json!", e);
        }
    }

    public void unload() {
        loaded = false;
        evaluator.stop();
        MinecraftServer.getInstanceManager().unregisterInstance(play);
        MinecraftServer.getInstanceManager().unregisterInstance(code);
    }

    public boolean isOwnerOrContributor(Player player) {
        return info.owner.equals(player.getUuid()) || info.contributors.contains(player.getUuid());
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (Stream<Path> paths = Files.list(path)) {
                Iterator<Path> iterator = paths.iterator();
                while (iterator.hasNext()) {
                    deleteRecursively(iterator.next());
                }
            }
        }
        Files.delete(path);
    }
}
