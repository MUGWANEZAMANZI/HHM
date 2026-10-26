package com.service;

import com.dao.AppointmentDAO;
import com.dao.DepartmentDAO;
import com.dao.DoctorDAO;
import com.dto.DoctorRequest;
import com.model.AppointmentView;
import com.model.Doctor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorDAO      doctorDAO;
    private final DepartmentDAO  departmentDAO;
    private final AppointmentDAO appointmentDAO;

    public DoctorService(DoctorDAO doctorDAO, DepartmentDAO departmentDAO, AppointmentDAO appointmentDAO) {
        this.doctorDAO      = doctorDAO;
        this.departmentDAO  = departmentDAO;
        this.appointmentDAO = appointmentDAO;
    }

    public void register(DoctorRequest req) throws ServiceException {
        if (departmentDAO.findById(req.getDepartmentId()) == null)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Department with id " + req.getDepartmentId() + " does not exist.");

        boolean licenseTaken = doctorDAO.findAll().stream()
            .anyMatch(d -> d.getLicenseNumber().equalsIgnoreCase(req.getLicenseNumber()));
        if (licenseTaken)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "License number \"" + req.getLicenseNumber() + "\" is already registered.");

        doctorDAO.insert(new Doctor(req.getName(), req.getDepartmentId(), req.getLicenseNumber()));
    }

    public void update(Doctor doctor) throws ServiceException {
        int rows = doctorDAO.update(doctor);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctor.getId() + " not found.");
    }

    // Business rule: cannot delete a doctor who has Scheduled appointments
    public void remove(long doctorId) throws ServiceException {
        List<AppointmentView> active = appointmentDAO.findByDoctor(doctorId).stream()
            .filter(a -> "Scheduled".equals(a.getStatus()))
            .toList();
        if (!active.isEmpty())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Cannot delete: doctor has " + active.size() + " scheduled appointment(s). Cancel them first.");
        int rows = doctorDAO.delete(doctorId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found.");
    }

    public List<Doctor> getAll() throws ServiceException {
        return doctorDAO.findAll();
    }

    public List<Doctor> search(String keyword) throws ServiceException {
        return doctorDAO.search(keyword);
    }
}
