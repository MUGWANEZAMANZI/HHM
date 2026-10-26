package dao;

import model.Appointment;
import model.AppointmentView;
import util.AppCache;
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
        AppCache.invalidate(AppointmentView.class);
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
        AppCache.invalidate(AppointmentView.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Appointments WHERE Id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(AppointmentView.class);
    }

    public List<AppointmentView> findAll() throws SQLException {
        List<AppointmentView> cached = AppCache.getAll(AppointmentView.class);
        if (cached != null) return cached;

        String sql = SELECT_VIEW + "ORDER BY a.appointmentDate DESC";
        List<AppointmentView> list;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            list = mapRows(stmt.executeQuery());
        }
        AppCache.put(AppointmentView.class, list);
        return list;
    }

    public AppointmentView findById(long id) throws SQLException {
        return findAll().stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    public List<AppointmentView> search(String keyword) throws SQLException {
        String lower = keyword.toLowerCase();
        return findAll().stream()
            .filter(a -> a.getPatientName().toLowerCase().contains(lower)
                      || a.getDoctorName().toLowerCase().contains(lower)
                      || a.getStatus().toLowerCase().contains(lower)
                      || (a.getPatientPhone() != null && a.getPatientPhone().contains(keyword)))
            .toList();
    }

    public List<AppointmentView> findByPatient(long patientId) throws SQLException {
        return findAll().stream().filter(a -> a.getPatientId() == patientId).toList();
    }

    public List<AppointmentView> findByDoctor(long doctorId) throws SQLException {
        return findAll().stream().filter(a -> a.getDoctorId() == doctorId).toList();
    }

    public List<AppointmentView> findByStatus(String status) throws SQLException {
        return findAll().stream().filter(a -> a.getStatus().equals(status)).toList();
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
