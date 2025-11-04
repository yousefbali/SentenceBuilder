package org.example.data;

import org.example.model.FileRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatasetRepository {

    public List<FileRecord> list() {
        String sql = "SELECT id, filename, path, word_count, import_date FROM files ORDER BY import_date DESC";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<FileRecord> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new FileRecord(
                        rs.getInt("id"),
                        rs.getString("filename"),
                        rs.getString("path"),
                        rs.getInt("word_count"),
                        LocalDate.parse(rs.getString("import_date"))
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public FileRecord insert(String filename, String path, int wordCount, LocalDate importDate) {
        String sql = "INSERT INTO files (filename, path, word_count, import_date) VALUES (?,?,?,?)";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, filename);
            ps.setString(2, path);
            ps.setInt(3, wordCount);
            ps.setString(4, importDate.toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new FileRecord(id, filename, path, wordCount, importDate);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM files WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
