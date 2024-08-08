package de.blazemcworld.fireflow.database;

import de.blazemcworld.fireflow.util.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class Database {

    private final Connection connection;

    public static Map<String, Object> tables;

    public Database() {
        try {
            connection = DriverManager.getConnection(Config.store.database().jdbcUrl(), Config.store.database().user(), Config.store.database().password());
            tables = Map.of(
                    "players", new PlayersTable(connection),
                    "spaces", new SpacesTable(connection),
                    "permissions", new PermissionsTable(connection)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
