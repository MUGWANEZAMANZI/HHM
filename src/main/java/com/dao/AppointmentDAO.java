package com.dao;

import com.model.Appointment;
import com.model.AppointmentView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AppointmentDAO {

    private final JdbcTemplate jdbc;

    public AppointmentDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_VIEW =
        "SELECT a.Id, a.patientId, a.doctorId, a.appointmentDate, a.status, " +
        "       p.Name AS patientName, p.Phone AS patientPhone, " +
        "       d.Name AS doctorName, dept.name AS departmentName " +
        "FROM Appointments a " +
        "INNER JOIN Patient     p    ON a.patientId   = p.Id " +
        "INNER JOIN Doctors     d    ON a.doctorId    = d.Id " +
        "INNER JOIN Departments dept ON d.departmentId = dept.id ";

    public void insert(Appointment a) {
        jdbc.update(
            "INSERT INTO Appointments (patientId, doctorId, appointmentDate, status) VALUES (?, ?, ?, ?)",
            a.getPatientId(), a.getDoctorId(), Timestamp.valueOf(a.getAppointmentDate()), a.getStatus());
    }

    public int update(Appointment a) {
        return jdbc.update(
            "UPDATE Appointments SET patientId=?, doctorId=?, appointmentDate=?, status=? WHERE Id=?",
            a.getPatientId(), a.getDoctorId(), Timestamp.valueOf(a.getAppointmentDate()),
            a.getStatus(), a.getId());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM Appointments WHERE Id=?", id);
    }

    public List<AppointmentView> findAll() {
        return jdbc.query(SELECT_VIEW, (rs, rowNum) -> mapRow(rs));
    }

    public AppointmentView findById(long id) {
        return findAll().stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    public List<AppointmentView> findByPatient(long patientId) {
        return findAll().stream().filter(a -> a.getPatientId() == patientId).toList();
    }

    public List<AppointmentView> findByDoctor(long doctorId) {
        return findAll().stream().filter(a -> a.getDoctorId() == doctorId).toList();
    }

    public List<AppointmentView> findByStatus(String status) {
        return findAll().stream().filter(a -> a.getStatus().equals(status)).toList();
    }

    public List<AppointmentView> search(String keyword) {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(a -> a.getPatientName().toLowerCase().contains(lower)
                      || a.getDoctorName().toLowerCase().contains(lower)
                      || a.getStatus().toLowerCase().contains(lower)
                      || (a.getPatientPhone() != null && a.getPatientPhone().contains(keyword)))
            .toList();
    }

    private AppointmentView mapRow(ResultSet rs) throws SQLException {
        AppointmentView v = new AppointmentView();
        v.setId(rs.getLong("Id"));
        v.setPatientId(rs.getLong("patientId"));
        v.setDoctorId(rs.getLong("doctorId"));
        v.setPatientName(rs.getString("patientName"));
        v.setPatientPhone(rs.getString("patientPhone"));
        v.setDoctorName(rs.getString("doctorName"));
        v.setDepartmentName(rs.getString("departmentName"));
        v.setAppointmentDate(rs.getTimestamp("appointmentDate").toLocalDateTime());
        v.setStatus(rs.getString("status"));
        return v;
    }
}
