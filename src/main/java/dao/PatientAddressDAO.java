package dao;

import model.PatientAddress;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        AppCache.invalidate(PatientAddress.class);
    }

    public PatientAddress findByPatient(long patientId) throws SQLException {
        List<PatientAddress> all = findAllList();
        return all.stream().filter(a -> a.getPatientId() == patientId).findFirst().orElse(null);
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
        AppCache.invalidate(PatientAddress.class);
    }

    public void deleteByPatient(long patientId) throws SQLException {
        String sql = "DELETE FROM `Patient Address` WHERE patientId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            stmt.executeUpdate();
        }
        AppCache.invalidate(PatientAddress.class);
    }

    // Returns map keyed by patientId — used by PatientController.toRows()
    public Map<Long, PatientAddress> findAll() throws SQLException {
        Map<Long, PatientAddress> map = new HashMap<>();
        for (PatientAddress a : findAllList()) map.put(a.getPatientId(), a);
        return map;
    }

    // -------------------------------------------------------------------------

    private List<PatientAddress> findAllList() throws SQLException {
        List<PatientAddress> cached = AppCache.getAll(PatientAddress.class);
        if (cached != null) return cached;

        List<PatientAddress> list = new ArrayList<>();
        String sql = "SELECT * FROM `Patient Address`";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        AppCache.put(PatientAddress.class, list);
        return list;
    }

    private PatientAddress mapRow(ResultSet rs) throws SQLException {
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
