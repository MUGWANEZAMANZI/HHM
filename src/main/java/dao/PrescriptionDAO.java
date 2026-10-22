package dao;

import model.Prescription;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDAO {

    public void insert(Prescription p) throws SQLException {
        String sql = "INSERT INTO Prescriptions (patientId, doctorId, issuedAt, status, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, p.getPatientId());
            stmt.setLong(2, p.getDoctorId());
            stmt.setTimestamp(3, Timestamp.valueOf(p.getIssuedAt()));
            stmt.setString(4, p.getStatus());
            stmt.setString(5, p.getNotes());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Prescription.class);
    }

    public List<Prescription> findAll() throws SQLException {
        List<Prescription> cached = AppCache.getAll(Prescription.class);
        if (cached != null) return cached;

        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT * FROM Prescriptions ORDER BY issuedAt DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        AppCache.put(Prescription.class, list);
        return list;
    }

    public Prescription findById(long id) throws SQLException {
        return findAll().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<Prescription> findByPatient(long patientId) throws SQLException {
        return findAll().stream().filter(p -> p.getPatientId() == patientId).toList();
    }

    public List<Prescription> findByDoctor(long doctorId) throws SQLException {
        return findAll().stream().filter(p -> p.getDoctorId() == doctorId).toList();
    }

    public void update(Prescription p) throws SQLException {
        String sql = "UPDATE Prescriptions SET patientId=?, doctorId=?, issuedAt=?, status=?, notes=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, p.getPatientId());
            stmt.setLong(2, p.getDoctorId());
            stmt.setTimestamp(3, Timestamp.valueOf(p.getIssuedAt()));
            stmt.setString(4, p.getStatus());
            stmt.setString(5, p.getNotes());
            stmt.setLong(6, p.getId());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Prescription.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Prescriptions WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(Prescription.class);
    }

    private Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getLong("Id"));
        p.setPatientId(rs.getLong("patientId"));
        p.setDoctorId(rs.getLong("doctorId"));
        p.setIssuedAt(rs.getTimestamp("issuedAt").toLocalDateTime());
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
