package com.dao;

import com.model.PrescriptionItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PrescriptionItemDAO {

    private final JdbcTemplate jdbc;

    public PrescriptionItemDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(PrescriptionItem item) {
        jdbc.update(
            "INSERT INTO PrescriptionItems (prescriptionId, medicationId, dosage, frequency, quantity, refillsAllowed) VALUES (?, ?, ?, ?, ?, ?)",
            item.getPrescriptionId(), item.getMedicationId(), item.getDosage(),
            item.getFrequency(), item.getQuantity(), item.getRefillsAllowed());
    }

    public void update(PrescriptionItem item) {
        jdbc.update(
            "UPDATE PrescriptionItems SET medicationId=?, dosage=?, frequency=?, quantity=?, refillsAllowed=? WHERE Id=?",
            item.getMedicationId(), item.getDosage(), item.getFrequency(),
            item.getQuantity(), item.getRefillsAllowed(), item.getId());
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM PrescriptionItems WHERE Id=?", id);
    }

    public List<PrescriptionItem> findAll() {
        return jdbc.query("SELECT * FROM PrescriptionItems", (rs, rowNum) -> mapRow(rs));
    }

    public List<PrescriptionItem> findByPrescription(long prescriptionId) {
        return findAll().stream()
            .filter(item -> item.getPrescriptionId() == prescriptionId)
            .toList();
    }

    private PrescriptionItem mapRow(ResultSet rs) throws SQLException {
        PrescriptionItem item = new PrescriptionItem();
        item.setId(rs.getLong("Id"));
        item.setPrescriptionId(rs.getLong("prescriptionId"));
        item.setMedicationId(rs.getLong("medicationId"));
        item.setDosage(rs.getString("dosage"));
        item.setFrequency(rs.getString("frequency"));
        item.setQuantity(rs.getInt("quantity"));
        item.setRefillsAllowed(rs.getInt("refillsAllowed"));
        return item;
    }
}
