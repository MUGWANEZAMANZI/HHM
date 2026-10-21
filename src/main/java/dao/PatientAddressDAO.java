package dao;

import model.PatientAddress;
import util.DBConnection;

import java.sql.*;

public class PatientAddressDAO {

    public void insert(PatientAddress a) throws SQLException {
        String sql = "INSERT INTO `Patient Address` (patientId, District, Sector, Cell, Village) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, a.getPatientId());
            stmt.setString(2, a.getDistrict());
            stmt.setString(3, a.getSector());
            stmt.setString(4, a.getCell());
            stmt.setString(5, a.getVillage());
            stmt.executeUpdate();
        }
    }

    public PatientAddress findByPatient(long patientId) throws SQLException {
        String sql = "SELECT * FROM `Patient Address` WHERE patientId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PatientAddress a = new PatientAddress();
                a.setId(rs.getLong("Id"));
                a.setPatientId(rs.getLong("patientId"));
                a.setDistrict(rs.getString("District"));
                a.setSector(rs.getString("Sector"));
                a.setCell(rs.getString("Cell"));
                a.setVillage(rs.getString("Village"));
                return a;
            }
        }
        return null;
    }

    public void update(PatientAddress a) throws SQLException {
        String sql = "UPDATE `Patient Address` SET District=?, Sector=?, Cell=?, Village=? WHERE patientId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getDistrict());
            stmt.setString(2, a.getSector());
            stmt.setString(3, a.getCell());
            stmt.setString(4, a.getVillage());
            stmt.setLong(5, a.getPatientId());
            stmt.executeUpdate();
        }
    }

    public void deleteByPatient(long patientId) throws SQLException {
        String sql = "DELETE FROM `Patient Address` WHERE patientId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            stmt.executeUpdate();
        }
    }
}
