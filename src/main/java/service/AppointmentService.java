package service;

import dao.AppointmentDAO;
import model.Appointment;
import model.AppointmentView;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AppointmentService {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    // Business rule: cannot book an appointment in the past
    public void book(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past.");
        try {
            appointmentDAO.insert(appointment);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void reschedule(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Rescheduled date cannot be in the past.");
        try {
            appointmentDAO.update(appointment);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    // Business rule: can only cancel appointments that are not already cancelled
    public void cancel(long appointmentId) throws ServiceException {
        try {
            AppointmentView existing = appointmentDAO.findById(appointmentId);
            if (existing == null)
                throw new ServiceException("Appointment not found.");
            if ("Cancelled".equals(existing.getStatus()))
                throw new ServiceException("Appointment is already cancelled.");

            Appointment a = new Appointment(
                existing.getPatientId(), existing.getDoctorId(),
                existing.getAppointmentDate(), "Cancelled");
            a.setId(appointmentId);
            appointmentDAO.update(a);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void remove(long appointmentId) throws ServiceException {
        try { appointmentDAO.delete(appointmentId); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<AppointmentView> getAll() throws ServiceException {
        try { return appointmentDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<AppointmentView> search(String keyword) throws ServiceException {
        try { return appointmentDAO.search(keyword); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<AppointmentView> findByStatus(String status) throws ServiceException {
        try { return appointmentDAO.findByStatus(status); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }
}
