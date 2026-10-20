package dao;

import model.PrescriptionItem;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionItemDAO {

    public void insert(PrescriptionItem item) throws SQLException {
        String sql = "INSERT INTO PrescriptionItems (prescriptionId, medicationId, dosage, frequency, quantity, refillsAllowed) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, item.getPrescriptionId());
            stmt.setLong(2, item.getMedicationId());
            stmt.setString(3, item.getDosage());
            stmt.setString(4, item.getFrequency());
            stmt.setInt(5, item.getQuantity());
            stmt.setInt(6, item.getRefillsAllowed());
            stmt.executeUpdate();
        }
    }

    public List<PrescriptionItem> findByPrescription(long prescriptionId) throws SQLException {
        List<PrescriptionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM PrescriptionItems WHERE prescriptionId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, prescriptionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void update(PrescriptionItem item) throws SQLException {
        String sql = "UPDATE PrescriptionItems SET medicationId=?, dosage=?, frequency=?, quantity=?, refillsAllowed=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, item.getMedicationId());
            stmt.setString(2, item.getDosage());
            stmt.setString(3, item.getFrequency());
            stmt.setInt(4, item.getQuantity());
            stmt.setInt(5, item.getRefillsAllowed());
            stmt.setLong(6, item.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM PrescriptionItems WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
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
