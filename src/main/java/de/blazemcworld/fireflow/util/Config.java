package de.blazemcworld.fireflow.util;

import com.google.gson.Gson;
import de.blazemcworld.fireflow.FireFlow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public static final Store store = readConfig();

    private static Store readConfig() {
        try {
            return new Gson().fromJson(Files.readString(Path.of("config.json")), Store.class);
        } catch (IOException e) {
            FireFlow.LOGGER.error("Error reading config.json!");
            throw new RuntimeException(e);
        }
    }

    public record Store(String motd, Database database) {}

    public record Database(String jdbcUrl, String user, String password) {}
}
