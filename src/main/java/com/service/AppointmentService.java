package com.service;

import com.dao.AppointmentDAO;
import com.model.Appointment;
import com.model.AppointmentView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentDAO appointmentDAO;

    //Constructor injection for AppointmentDAO
    public AppointmentService(AppointmentDAO appointmentDAO)
    {
        this.appointmentDAO = appointmentDAO;
    }
    // Business rule: cannot book an appointment in the past
    public void book(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past.");
        appointmentDAO.insert(appointment);
    }

    public void reschedule(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Rescheduled date cannot be in the past.");
        int rows = appointmentDAO.update(appointment);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointment.getId() + " not found.");
    }

    // Business rule: can only cancel appointments that are not already cancelled
    public void cancel(long appointmentId) throws ServiceException {
        AppointmentView existing = appointmentDAO.findById(appointmentId);
        if (existing == null)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointmentId + " not found.");
        if ("Cancelled".equals(existing.getStatus()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Appointment is already cancelled.");

        Appointment a = new Appointment(
            existing.getPatientId(), existing.getDoctorId(),
            existing.getAppointmentDate(), "Cancelled");
        a.setId(appointmentId);
        appointmentDAO.update(a);
    }

    public void remove(long appointmentId) throws ServiceException {
        int rows = appointmentDAO.delete(appointmentId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointmentId + " not found.");
    }

    public List<AppointmentView> getAll() throws ServiceException {
        return appointmentDAO.findAll();
    }

    public List<AppointmentView> search(String keyword) throws ServiceException {
        return appointmentDAO.search(keyword);
    }

    public List<AppointmentView> findByStatus(String status) throws ServiceException {
        return appointmentDAO.findByStatus(status);
    }
}
