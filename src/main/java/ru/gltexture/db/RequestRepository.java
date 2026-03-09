package ru.gltexture.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RequestRepository {
    public static void saveRequest(long userId, String hash, byte[] image, String text) {
        String sql = """
            INSERT INTO requests
            (user_id, image_hash, image_data, recognized_text, status)
            VALUES (?, ?, ?, ?, 'done')
            """;
        try (Connection c = Database.get().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, hash);
            ps.setBytes(3, image);
            ps.setString(4, text);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String findByHash(String hash) {
        String sql = """
            SELECT recognized_text
            FROM requests
            WHERE image_hash = ?
            AND status = 'done'
            LIMIT 1
            """;
        try (Connection c = Database.get().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getHistory(long userId) {
        String sql = """
            SELECT recognized_text
            FROM requests
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT 10
            """;
        List<String> result = new ArrayList<>();
        try (Connection c = Database.get().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}