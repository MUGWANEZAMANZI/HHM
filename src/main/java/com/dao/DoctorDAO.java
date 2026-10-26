package com.dao;

import com.model.Doctor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DoctorDAO {

    private final JdbcTemplate jdbc;

    public DoctorDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Doctor doctor) {
        jdbc.update("INSERT INTO Doctors (Name, departmentId, licenseNumber) VALUES (?, ?, ?)",
            doctor.getName(), doctor.getDepartmentId(), doctor.getLicenseNumber());
    }

    public int update(Doctor doctor) {
        return jdbc.update("UPDATE Doctors SET Name=?, departmentId=?, licenseNumber=? WHERE Id=?",
            doctor.getName(), doctor.getDepartmentId(), doctor.getLicenseNumber(), doctor.getId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM Doctors WHERE Id=?", id);
    }

    public List<Doctor> findAll() {
        return jdbc.query("SELECT * FROM Doctors ORDER BY Name", (rs, rowNum) -> mapRow(rs));
    }

    public Doctor findById(long id) {
        return findAll().stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    public List<Doctor> findByDepartment(long departmentId) {
        return findAll().stream().filter(d -> d.getDepartmentId() == departmentId).toList();
    }

    public List<Doctor> search(String keyword) {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(d -> d.getName().toLowerCase().contains(lower)
                      || d.getLicenseNumber().toLowerCase().contains(lower))
            .toList();
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
