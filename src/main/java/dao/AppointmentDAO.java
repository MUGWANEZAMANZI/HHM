package dao;

import model.Appointment;
import model.AppointmentView;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    private static final String SELECT_VIEW =
        "SELECT a.Id, a.patientId, a.doctorId, a.appointmentDate, a.status, " +
        "       p.Name AS patientName, p.Phone AS patientPhone, " +
        "       d.Name AS doctorName, dept.name AS departmentName " +
        "FROM Appointments a " +
        "INNER JOIN Patient     p    ON a.patientId   = p.Id " +
        "INNER JOIN Doctors     d    ON a.doctorId    = d.Id " +
        "INNER JOIN Departments dept ON d.departmentId = dept.id ";

    public void insert(Appointment a) throws SQLException {
        String sql = "INSERT INTO Appointments (patientId, doctorId, appointmentDate, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, a.getPatientId());
            stmt.setLong(2, a.getDoctorId());
            stmt.setTimestamp(3, Timestamp.valueOf(a.getAppointmentDate()));
            stmt.setString(4, a.getStatus());
            stmt.executeUpdate();
        }
    }

    public void update(Appointment a) throws SQLException {
        String sql = "UPDATE Appointments SET patientId=?, doctorId=?, appointmentDate=?, status=? WHERE Id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, a.getPatientId());
            stmt.setLong(2, a.getDoctorId());
            stmt.setTimestamp(3, Timestamp.valueOf(a.getAppointmentDate()));
            stmt.setString(4, a.getStatus());
            stmt.setLong(5, a.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Appointments WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public List<AppointmentView> findAll() throws SQLException {
        String sql = SELECT_VIEW + "ORDER BY a.appointmentDate DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return mapRows(stmt.executeQuery());
        }
    }

    public AppointmentView findById(long id) throws SQLException {
        String sql = SELECT_VIEW + "WHERE a.Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            List<AppointmentView> result = mapRows(stmt.executeQuery());
            return result.isEmpty() ? null : result.get(0);
        }
    }

    public List<AppointmentView> search(String keyword) throws SQLException {
        String sql = SELECT_VIEW +
            "WHERE p.Name LIKE ? OR d.Name LIKE ? OR a.status LIKE ? " +
            "ORDER BY a.appointmentDate DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String term = "%" + keyword + "%";
            stmt.setString(1, term);
            stmt.setString(2, term);
            stmt.setString(3, term);
            return mapRows(stmt.executeQuery());
        }
    }

    public List<AppointmentView> findByDoctor(long doctorId) throws SQLException {
        String sql = SELECT_VIEW + "WHERE a.doctorId = ? ORDER BY a.appointmentDate DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, doctorId);
            return mapRows(stmt.executeQuery());
        }
    }

    public List<AppointmentView> findByPatient(long patientId) throws SQLException {
        String sql = SELECT_VIEW + "WHERE a.patientId = ? ORDER BY a.appointmentDate DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            return mapRows(stmt.executeQuery());
        }
    }

    public List<AppointmentView> findByStatus(String status) throws SQLException {
        String sql = SELECT_VIEW + "WHERE a.status = ? ORDER BY a.appointmentDate DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            return mapRows(stmt.executeQuery());
        }
    }

    private List<AppointmentView> mapRows(ResultSet rs) throws SQLException {
        List<AppointmentView> list = new ArrayList<>();
        while (rs.next()) {
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
            list.add(v);
        }
        return list;
    }
}
