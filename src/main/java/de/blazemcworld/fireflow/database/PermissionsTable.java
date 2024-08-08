package de.blazemcworld.fireflow.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class PermissionsTable {

    private final Connection connection;

    public PermissionsTable(Connection connection) {
        this.connection = connection;
        try {
            connection.prepareStatement("""
                    CREATE TABLE
                    IF NOT EXISTS
                    permissions (
                        id INT(11) NOT NULL AUTO_INCREMENT,
                        player INT(11) NOT NULL,
                        space INT(11) NOT NULL,
                        role INT(11) NOT NULL,
                        PRIMARY KEY (id),
                        FOREIGN KEY (player) REFERENCES players(id),
                        FOREIGN KEY (space) REFERENCES spaces(id)
                    )
                    """).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getRole(String player_uuid, int space) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT role
                    FROM permissions
                    JOIN players
                        ON players.id = permissions.player
                    WHERE players.uuid = ? AND space = ?
                    """);
            query.setString(1, player_uuid);
            query.setInt(2, space);
            boolean hasResult = query.execute();
            if (!hasResult) return 0;
            return query.getResultSet().next() ? query.getResultSet().getInt("role") : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getRoles(int space) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT players.uuid, role
                    FROM permissions
                    JOIN players
                        ON players.id = permissions.player
                    WHERE space = ?
                    """);
            query.setInt(1, space);
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            Map<String, String> result = new java.util.HashMap<>(Map.of());
            while (query.getResultSet().next()) {
                result.put(query.getResultSet().getString("uuid"), query.getResultSet().getString("role"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRole(String player_uuid, int space, int role) {
        try {
            int playerId = (int) ((PlayersTable) Database.tables.get("players")).getByUUID(player_uuid).get("id");
            PreparedStatement query = connection.prepareStatement("""
                    INSERT INTO permissions
                    (player, space, role)
                    VALUES
                    (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    role = VALUES(role)
                    """);
            query.setInt(1, playerId);
            query.setInt(2, space);
            query.setInt(3, role);
            query.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, Map<String, Object>> getOwnedSpaces(String uuid) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT *
                    FROM permissions
                    JOIN spaces
                        ON spaces.id = permissions.space
                    JOIN players
                        ON players.id = permissions.player
                    WHERE players.uuid = ?
                        AND role = 1
                    """);
            query.setString(1, uuid);
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            Map<Integer, Map<String, Object>> result = new java.util.HashMap<>(Map.of());
            while (query.getResultSet().next()) {
                result.put(query.getResultSet().getInt("space"), Map.of(
                        "id", query.getResultSet().getInt("space"),
                        "title", query.getResultSet().getString("title"),
                        "icon", query.getResultSet().getString("icon")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}