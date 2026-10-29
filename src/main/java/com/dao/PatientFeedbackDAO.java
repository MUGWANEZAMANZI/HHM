package com.dao;

import com.model.PatientFeedback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class PatientFeedbackDAO {

    private final JdbcTemplate jdbc;

    public PatientFeedbackDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(PatientFeedback f) {
        jdbc.update(
            "INSERT INTO PatientFeedback (patientId, Feedback, passwordNotes, issuedAt) VALUES (?, ?, ?, ?)",
            f.getPatientId(), f.getFeedback(), f.getPrivateNotes(),
            Timestamp.valueOf(f.getIssuedAt()));
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM PatientFeedback WHERE Id=?", id);
    }

    public List<PatientFeedback> findAll() {
        return jdbc.query("SELECT * FROM PatientFeedback ORDER BY issuedAt DESC",
            (rs, rowNum) -> mapRow(rs));
    }

    public List<PatientFeedback> findByPatient(long patientId) {
        return findAll().stream().filter(f -> f.getPatientId() == patientId).toList();
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
