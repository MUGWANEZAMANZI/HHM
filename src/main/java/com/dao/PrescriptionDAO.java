package com.dao;

import com.model.Prescription;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class PrescriptionDAO {

    private final JdbcTemplate jdbc;

    public PrescriptionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Prescription p) {
        jdbc.update(
            "INSERT INTO Prescriptions (patientId, doctorId, issuedAt, status, notes) VALUES (?, ?, ?, ?, ?)",
            p.getPatientId(), p.getDoctorId(), Timestamp.valueOf(p.getIssuedAt()),
            p.getStatus(), p.getNotes());
    }

    public int update(Prescription p) {
        return jdbc.update(
            "UPDATE Prescriptions SET patientId=?, doctorId=?, issuedAt=?, status=?, notes=? WHERE Id=?",
            p.getPatientId(), p.getDoctorId(), Timestamp.valueOf(p.getIssuedAt()),
            p.getStatus(), p.getNotes(), p.getId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM Prescriptions WHERE Id=?", id);
    }

    public List<Prescription> findAll() {
        return jdbc.query("SELECT * FROM Prescriptions ORDER BY issuedAt DESC",
            (rs, rowNum) -> mapRow(rs));
    }

    public Prescription findById(long id) {
        return findAll().stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<Prescription> findByPatient(long patientId) {
        return findAll().stream().filter(p -> p.getPatientId() == patientId).toList();
    }

    public List<Prescription> findByDoctor(long doctorId) {
        return findAll().stream().filter(p -> p.getDoctorId() == doctorId).toList();
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
