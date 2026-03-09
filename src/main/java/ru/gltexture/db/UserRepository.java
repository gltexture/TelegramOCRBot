package ru.gltexture.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    public static long getOrCreateUser(long telegramId) {
        String sql = """
            INSERT INTO users (telegram_id)
            VALUES (?)
            ON CONFLICT (telegram_id)
            DO UPDATE SET telegram_id = EXCLUDED.telegram_id
            RETURNING id
            """;

        try (Connection c = Database.get().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, telegramId);

            ResultSet rs = ps.executeQuery();
            rs.next();

            return rs.getLong(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}