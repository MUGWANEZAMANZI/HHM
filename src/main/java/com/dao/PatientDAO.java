package com.dao;

import com.model.Patient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class PatientDAO {

    private final JdbcTemplate jdbc;

    public PatientDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertAndGetId(Patient patient) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Patient (Name, Address, Phone, email) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getAddress());
            stmt.setString(3, patient.getPhone());
            stmt.setString(4, patient.getEmail());
            return stmt;
        }, key);
        return key.getKey().longValue();
    }

    public void insert(Patient patient) {
        jdbc.update("INSERT INTO Patient (Name, Address, Phone, email) VALUES (?, ?, ?, ?)",
            patient.getName(), patient.getAddress(), patient.getPhone(), patient.getEmail());
    }

    public List<Patient> findAll() {
        return jdbc.query("SELECT * FROM Patient ORDER BY Name", (rs, rowNum) -> mapRow(rs));
    }

    public Patient findById(long id) {
        return findAll().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public int update(Patient patient) {
        return jdbc.update("UPDATE Patient SET Name=?, Address=?, Phone=?, email=? WHERE Id=?",
            patient.getName(), patient.getAddress(), patient.getPhone(), patient.getEmail(), patient.getId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM Patient WHERE Id=?", id);
    }

    public List<Patient> search(String keyword) {
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
