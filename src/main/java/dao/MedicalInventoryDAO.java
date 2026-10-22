package dao;

import model.MedicalInventory;
import util.AppCache;
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
        AppCache.invalidate(MedicalInventory.class);
    }

    public List<MedicalInventory> findAll() throws SQLException {
        List<MedicalInventory> cached = AppCache.getAll(MedicalInventory.class);
        if (cached != null) return cached;

        List<MedicalInventory> list = new ArrayList<>();
        String sql = "SELECT * FROM MedicalInventory ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        AppCache.put(MedicalInventory.class, list);
        return list;
    }

    public MedicalInventory findById(long id) throws SQLException {
        return findAll().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    public List<MedicalInventory> findLowStock() throws SQLException {
        return findAll().stream()
            .filter(m -> m.getQuantityOnHand() <= m.getReorderLevel())
            .toList();
    }

    public List<MedicalInventory> search(String keyword) throws SQLException {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(m -> m.getName().toLowerCase().contains(lower)
                      || m.getSku().toLowerCase().contains(lower))
            .toList();
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
        AppCache.invalidate(MedicalInventory.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM MedicalInventory WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(MedicalInventory.class);
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
