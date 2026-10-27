package com.dao;

import com.model.MedicalItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MedicalItemDAO {

    private final JdbcTemplate jdbc;

    public MedicalItemDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MedicalItem> findByInventoryId(long inventoryId) {
        return jdbc.query(
            "SELECT * FROM MedicalItems WHERE inventoryId = ?",
            (rs, rowNum) -> mapRow(rs), inventoryId);
    }

    /** Deletes all MedicalItems linked to the given inventory row.
     *  Called by MedicalInventoryService.remove() before deleting the parent. */
    public int deleteByInventoryId(long inventoryId) {
        return jdbc.update("DELETE FROM MedicalItems WHERE inventoryId = ?", inventoryId);
    }

    private MedicalItem mapRow(ResultSet rs) throws SQLException {
        MedicalItem m = new MedicalItem();
        m.setId(rs.getLong("id"));
        m.setInventoryId(rs.getLong("inventoryId"));
        m.setQuantity(rs.getInt("quantity"));
        m.setCategory(rs.getString("category"));
        return m;
    }
}
