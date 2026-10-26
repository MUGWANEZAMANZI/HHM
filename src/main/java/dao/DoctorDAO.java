package dao;

import model.Doctor;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class DoctorDAO {

    public void insert(Doctor doctor) throws SQLException {
        String sql = "INSERT INTO Doctors (Name, departmentId, licenseNumber) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doctor.getName());
            stmt.setLong(2, doctor.getDepartmentId());
            stmt.setString(3, doctor.getLicenseNumber());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Doctor.class);
    }

    public List<Doctor> findAll() throws SQLException {
        List<Doctor> cached = AppCache.getAll(Doctor.class);
        if (cached != null) return cached;

        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT * FROM Doctors ORDER BY Name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        AppCache.put(Doctor.class, list);
        return list;
    }

    public Doctor findById(long id) throws SQLException {
        return findAll().stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    public List<Doctor> findByDepartment(long departmentId) throws SQLException {
        return findAll().stream()
            .filter(d -> d.getDepartmentId() == departmentId)
            .toList();
    }

    public List<Doctor> search(String keyword) throws SQLException {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(d -> d.getName().toLowerCase().contains(lower)
                      || d.getLicenseNumber().toLowerCase().contains(lower))
            .toList();
    }

    public void update(Doctor doctor) throws SQLException {
        String sql = "UPDATE Doctors SET Name=?, departmentId=?, licenseNumber=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doctor.getName());
            stmt.setLong(2, doctor.getDepartmentId());
            stmt.setString(3, doctor.getLicenseNumber());
            stmt.setLong(4, doctor.getId());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Doctor.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Doctors WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(Doctor.class);
    }

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getLong("Id"));
        d.setName(rs.getString("Name"));
        d.setDepartmentId(rs.getLong("departmentId"));
        d.setLicenseNumber(rs.getString("licenseNumber"));
        return d;
    }
}
