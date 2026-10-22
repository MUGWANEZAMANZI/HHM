package dao;

import model.Patient;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public long insertAndGetId(Patient patient) throws SQLException {
        String sql = "INSERT INTO Patient (Name, Address, Phone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getAddress());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) { AppCache.invalidate(Patient.class); return keys.getLong(1); }
        }
        throw new SQLException("No generated key returned for Patient.");
    }

    public void insert(Patient patient) throws SQLException {
        String sql = "INSERT INTO Patient (Name, Address, Phone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getAddress());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Patient.class);
    }

    public List<Patient> findAll() throws SQLException {
        List<Patient> cached = AppCache.getAll(Patient.class);
        if (cached != null) return cached;

        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM Patient ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) patients.add(mapRow(rs));
        }
        AppCache.put(Patient.class, patients);
        return patients;
    }

    public Patient findById(long id) throws SQLException {
        return findAll().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public void update(Patient patient) throws SQLException {
        String sql = "UPDATE Patient SET Name=?, Address=?, Phone=?, email=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getAddress());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            stmt.setLong(5, patient.getId());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Patient.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Patient WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(Patient.class);
    }

    public List<Patient> search(String keyword) throws SQLException {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(p -> p.getName().toLowerCase().contains(lower)
                      || (p.getEmail() != null && p.getEmail().toLowerCase().contains(lower))
                      || (p.getPhone() != null && p.getPhone().contains(keyword)))
            .toList();
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getLong("Id"));
        p.setName(rs.getString("Name"));
        p.setAddress(rs.getString("Address"));
        p.setPhone(rs.getString("Phone"));
        p.setEmail(rs.getString("email"));
        return p;
    }
}
