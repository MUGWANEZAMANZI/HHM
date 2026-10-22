package service;

import dao.AppointmentDAO;
import dao.DoctorDAO;
import model.AppointmentView;
import model.Doctor;

import java.sql.SQLException;
import java.util.List;

public class DoctorService {

    private final DoctorDAO      doctorDAO      = new DoctorDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    // Business rule: license number must be unique
    public void register(Doctor doctor) throws ServiceException {
        try {
            boolean licenseTaken = doctorDAO.findAll().stream()
                .anyMatch(d -> d.getLicenseNumber().equalsIgnoreCase(doctor.getLicenseNumber()));
            if (licenseTaken)
                throw new ServiceException(
                    "License number \"" + doctor.getLicenseNumber() + "\" is already registered.");
            doctorDAO.insert(doctor);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void update(Doctor doctor) throws ServiceException {
        try { doctorDAO.update(doctor); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    // Business rule: cannot delete a doctor who has Scheduled appointments
    public void remove(long doctorId) throws ServiceException {
        try {
            List<AppointmentView> active = appointmentDAO.findByDoctor(doctorId).stream()
                .filter(a -> "Scheduled".equals(a.getStatus()))
                .toList();
            if (!active.isEmpty())
                throw new ServiceException(
                    "Cannot delete: doctor has " + active.size() + " scheduled appointment(s). Cancel them first.");
            doctorDAO.delete(doctorId);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public List<Doctor> getAll() throws ServiceException {
        try { return doctorDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<Doctor> search(String keyword) throws ServiceException {
        try { return doctorDAO.search(keyword); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }
}
