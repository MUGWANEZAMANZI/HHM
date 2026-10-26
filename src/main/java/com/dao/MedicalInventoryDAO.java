package com.dao;

import com.model.MedicalInventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MedicalInventoryDAO {

    private final JdbcTemplate jdbc;

    public MedicalInventoryDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(MedicalInventory item) {
        jdbc.update(
            "INSERT INTO MedicalInventory (name, sku, quantityOnHand, reorderLevel) VALUES (?, ?, ?, ?)",
            item.getName(), item.getSku(), item.getQuantityOnHand(), item.getReorderLevel());
    }

    public int update(MedicalInventory item) {
        return jdbc.update(
            "UPDATE MedicalInventory SET name=?, sku=?, quantityOnHand=?, reorderLevel=? WHERE Id=?",
            item.getName(), item.getSku(), item.getQuantityOnHand(), item.getReorderLevel(), item.getId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM MedicalInventory WHERE Id=?", id);
    }

    public List<MedicalInventory> findAll() {
        return jdbc.query("SELECT * FROM MedicalInventory ORDER BY name",
            (rs, rowNum) -> mapRow(rs));
    }

    public MedicalInventory findById(long id) {
        return findAll().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    public List<MedicalInventory> findLowStock() {
        return findAll().stream()
            .filter(m -> m.getQuantityOnHand() <= m.getReorderLevel())
            .toList();
    }

    public List<MedicalInventory> search(String keyword) {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(m -> m.getName().toLowerCase().contains(lower)
                      || m.getSku().toLowerCase().contains(lower))
            .toList();
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
