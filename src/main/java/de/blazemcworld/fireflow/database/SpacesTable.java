package de.blazemcworld.fireflow.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SpacesTable {

    private final Connection connection;

    public SpacesTable(Connection connection) {
        this.connection = connection;
        try {
            connection.prepareStatement("""
                    CREATE TABLE
                    IF NOT EXISTS
                    spaces (
                        id INT(11) NOT NULL AUTO_INCREMENT,
                        title VARCHAR(256) NOT NULL,
                        icon VARCHAR(256) NOT NULL DEFAULT '"paper"',
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
                    FROM spaces
                    WHERE id = ?
                    """);
            query.setInt(1, id);
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            return query.getResultSet().next() ? Map.of(
                    "id", query.getResultSet().getInt("id"),
                    "title", query.getResultSet().getString("title"),
                    "icon", query.getResultSet().getString("icon")
            ) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, List<String>> findByTitle(String title) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    SELECT *
                    FROM spaces
                    WHERE title LIKE ?
                    """);
            query.setString(1, "%" + title + "%");
            boolean hasResult = query.execute();
            if (!hasResult) return null;
            Map<String, List<String>> result = new java.util.HashMap<>(Map.of());
            while (query.getResultSet().next()) {
                result.put(query.getResultSet().getInt("id") + "", List.of(
                        query.getResultSet().getString("title"),
                        query.getResultSet().getString("icon")
                ));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean createSpace(String title, String icon) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    INSERT INTO spaces
                    (title, icon)
                    VALUES
                    (?, ?)
                    """);
            query.setString(1, title);
            query.setString(2, icon);
            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int createSpace(String title) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    INSERT INTO spaces
                    (title)
                    VALUES
                    (?)
                    """, PreparedStatement.RETURN_GENERATED_KEYS);
            query.setString(1, title);
            query.executeUpdate();
            ResultSet generatedKeys = query.getGeneratedKeys();
            generatedKeys.next();
            return generatedKeys.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean setName(int id, String title) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    UPDATE spaces
                    SET title = ?
                    WHERE id = ?
                    """);
            query.setString(1, title);
            query.setInt(2, id);
            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean setIcon(int id, String icon) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    UPDATE spaces
                    SET icon = ?
                    WHERE id = ?
                    """);
            query.setString(1, icon);
            query.setInt(2, id);
            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updateSpace(int id, String title, String icon) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    UPDATE spaces
                    SET title = ?, icon = ?
                    WHERE id = ?
                    """);
            query.setString(1, title);
            query.setString(2, icon);
            query.setInt(3, id);
            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteSpace(int id) {
        try {
            PreparedStatement query = connection.prepareStatement("""
                    DELETE FROM spaces
                    WHERE id = ?
                    """);
            query.setInt(1, id);
            return query.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
