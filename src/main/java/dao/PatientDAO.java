package dao;

import model.Patient;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

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
    }

    public List<Patient> findAll() throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM Patient";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Patient p = new Patient();
                p.setId(rs.getLong("Id"));
                p.setName(rs.getString("Name"));
                p.setAddress(rs.getString("Address"));
                p.setPhone(rs.getString("Phone"));
                p.setEmail(rs.getString("email"));
                patients.add(p);
            }
        }
        return patients;
    }

    public Patient findById(long id) throws SQLException {
        String sql = "SELECT * FROM Patient WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Patient p = new Patient();
                p.setId(rs.getLong("Id"));
                p.setName(rs.getString("Name"));
                p.setAddress(rs.getString("Address"));
                p.setPhone(rs.getString("Phone"));
                p.setEmail(rs.getString("email"));
                return p;
            }
        }
        return null;
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
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Patient WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public List<Patient> search(String keyword) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM Patient WHERE Name LIKE ? OR email LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            stmt.setString(1, term);
            stmt.setString(2, term);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Patient p = new Patient();
                p.setId(rs.getLong("Id"));
                p.setName(rs.getString("Name"));
                p.setAddress(rs.getString("Address"));
                p.setPhone(rs.getString("Phone"));
                p.setEmail(rs.getString("email"));
                patients.add(p);
            }
        }
        return patients;
    }
}
