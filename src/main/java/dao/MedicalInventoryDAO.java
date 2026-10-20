package dao;

import model.MedicalInventory;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalInventoryDAO {

    public void insert(MedicalInventory item) throws SQLException {
        String sql = "INSERT INTO MedicalInventory (name, sku, quantityOnHand, reorderLevel) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getSku());
            stmt.setInt(3, item.getQuantityOnHand());
            stmt.setInt(4, item.getReorderLevel());
            stmt.executeUpdate();
        }
    }

    public List<MedicalInventory> findAll() throws SQLException {
        List<MedicalInventory> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalInventory ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public MedicalInventory findById(long id) throws SQLException {
        String sql = "SELECT * FROM MedicalInventory WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    // Items that have fallen below reorder level
    public List<MedicalInventory> findLowStock() throws SQLException {
        List<MedicalInventory> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalInventory WHERE quantityOnHand <= reorderLevel ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<MedicalInventory> search(String keyword) throws SQLException {
        List<MedicalInventory> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalInventory WHERE name LIKE ? OR sku LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            stmt.setString(1, term);
            stmt.setString(2, term);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void update(MedicalInventory item) throws SQLException {
        String sql = "UPDATE MedicalInventory SET name=?, sku=?, quantityOnHand=?, reorderLevel=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getSku());
            stmt.setInt(3, item.getQuantityOnHand());
            stmt.setInt(4, item.getReorderLevel());
            stmt.setLong(5, item.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM MedicalInventory WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private MedicalInventory mapRow(ResultSet rs) throws SQLException {
        MedicalInventory m = new MedicalInventory();
        m.setId(rs.getLong("Id"));
        m.setName(rs.getString("name"));
        m.setSku(rs.getString("sku"));
        m.setQuantityOnHand(rs.getInt("quantityOnHand"));
        m.setReorderLevel(rs.getInt("reorderLevel"));
        return m;
    }
}
