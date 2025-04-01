package de.blazemcworld.fireflow;

import de.blazemcworld.fireflow.code.node.NodeList;
import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.command.*;
import de.blazemcworld.fireflow.space.Lobby;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.ping.ResponseData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FireFlow {

    public static final Logger LOGGER = LogManager.getLogger("FireFlow");
    private static final Component motd = MiniMessage.miniMessage().deserialize(Config.store.motd());

    public static void main(String[] args) {
        LOGGER.info("Starting...");
        MinecraftServer server = MinecraftServer.init();

        MinecraftServer.setBrandName("FireFlow");
        MojangAuth.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(CustomPlayer::new);

        Translations.init();
        NodeList.init();
        AllTypes.init();
        Lobby.init();
        SpaceManager.init();
        TextWidth.init();

        MinecraftServer.getCommandManager().register(
                new CodeCommand(),
                new JoinCommand(),
                new PlayCommand(),
                new ReloadCommand(),
                new LocateCommand(),
                new LobbyCommand(),
                new FunctionCommand(),
                new SpaceCommand(),
                new SnippetCommand(),
                new AddNodeCommand(),
                new DebugCommand(),
                new BuildCommand()
        );

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(Lobby.instance);
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
        });

        events.addListener(PlayerDisconnectEvent.class, event -> {
            events.call(new PlayerExitInstanceEvent(event.getPlayer()));
        });

        events.addListener(PlayerDeathEvent.class, event -> {
            event.setChatMessage(null);
        });

        events.addListener(ServerListPingEvent.class, event -> {
            ResponseData res = new ResponseData();
            res.setDescription(motd);

            try {
                Path favicon = Path.of("favicon.png");
                if (Files.exists(favicon))
                    res.setFavicon("data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(favicon)));
            } catch (IOException e) {
                LOGGER.error(e);
            }

            event.setResponseData(res);
        });

        server.start("0.0.0.0", Config.store.port());
        LOGGER.info("Ready!");
    }

}
