package de.blazemcworld.fireflow.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class PlayersTable {

    private final Connection connection;

    public PlayersTable(Connection connection) {
        this.connection = connection;
        try {
            connection.prepareStatement("""
                    CREATE TABLE
                    IF NOT EXISTS
                    players (
                        id INT(11) NOT NULL AUTO_INCREMENT,
                        name VARCHAR(16) NOT NULL,
                        uuid UUID NOT NULL,
                        PRIMARY KEY (id)
                    )
                    """).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getById(int id) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT *
                    FROM players
                    WHERE id = ?
                    """);
            query.setInt(1, id);
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            return query.getResultSet().next() ? Map.of(
                    "id", query.getResultSet().getInt("id"),
                    "name", query.getResultSet().getString("name"),
                    "uuid", query.getResultSet().getString("uuid")
            ) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getByUUID(String uuid) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT *
                    FROM players
                    WHERE uuid = ?
                    """);
            query.setString(1, uuid);
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            return query.getResultSet().next() ? Map.of(
                    "id", query.getResultSet().getInt("id"),
                    "name", query.getResultSet().getString("name"),
                    "uuid", query.getResultSet().getString("uuid")
            ) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean setName(String uuid, String name) {
        try {
            if (getByUUID(uuid) == null) {
                PreparedStatement query = connection.prepareStatement("""
                        INSERT INTO players
                        (name, uuid)
                        VALUES
                        (?, ?)
                        """);
                query.setString(1, name);
                query.setString(2, uuid);
                return query.executeUpdate() > 0;
            } else {
                PreparedStatement query = connection.prepareStatement("""
                        UPDATE players
                        SET name = ?
                        WHERE uuid = ?
                        """);
                query.setString(1, name);
                query.setString(2, uuid);
                return query.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
