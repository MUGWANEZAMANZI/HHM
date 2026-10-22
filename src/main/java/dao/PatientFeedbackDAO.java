package dao;

import model.PatientFeedback;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientFeedbackDAO {

    public void insert(PatientFeedback feedback) throws SQLException {
        String sql = "INSERT INTO PatientFeedback (patientId, Feedback, passwordNotes, issuedAt) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, feedback.getPatientId());
            stmt.setString(2, feedback.getFeedback());
            stmt.setString(3, feedback.getPrivateNotes());
            stmt.setTimestamp(4, Timestamp.valueOf(feedback.getIssuedAt()));
            stmt.executeUpdate();
        }
        AppCache.invalidate(PatientFeedback.class);
    }

    public List<PatientFeedback> findAll() throws SQLException {
        List<PatientFeedback> cached = AppCache.getAll(PatientFeedback.class);
        if (cached != null) return cached;

        List<PatientFeedback> list = new ArrayList<>();
        String sql = "SELECT * FROM PatientFeedback ORDER BY issuedAt DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        AppCache.put(PatientFeedback.class, list);
        return list;
    }

    public List<PatientFeedback> findByPatient(long patientId) throws SQLException {
        return findAll().stream().filter(f -> f.getPatientId() == patientId).toList();
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM PatientFeedback WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(PatientFeedback.class);
    }

    private PatientFeedback mapRow(ResultSet rs) throws SQLException {
        PatientFeedback f = new PatientFeedback();
        f.setId(rs.getLong("Id"));
        f.setPatientId(rs.getLong("patientId"));
        f.setFeedback(rs.getString("Feedback"));
        f.setPrivateNotes(rs.getString("passwordNotes"));
        f.setIssuedAt(rs.getTimestamp("issuedAt").toLocalDateTime());
        return f;
    }
}
