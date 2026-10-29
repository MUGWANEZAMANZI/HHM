package com.service;

import com.dto.DoctorRequest;
import com.model.Appointment;
import com.model.Doctor;
import com.repository.AppointmentRepository;
import com.repository.DepartmentRepository;
import com.repository.DoctorRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         DepartmentRepository departmentRepository,
                         AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    @CacheEvict(value = "doctors", allEntries = true)
    public void register(DoctorRequest req) throws ServiceException {
        if (!departmentRepository.existsById(req.getDepartmentId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Department with id " + req.getDepartmentId() + " does not exist.");

        boolean licenseTaken = doctorRepository.findAll().stream()
            .anyMatch(d -> d.getLicenseNumber().equalsIgnoreCase(req.getLicenseNumber()));
        if (licenseTaken)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "License number \"" + req.getLicenseNumber() + "\" is already registered.");

        doctorRepository.save(new Doctor(req.getName(), req.getDepartmentId(), req.getLicenseNumber()));
    }

    @Transactional
    @CacheEvict(value = "doctors", allEntries = true)
    public void update(Doctor doctor) throws ServiceException {
        if (!doctorRepository.existsById(doctor.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctor.getId() + " not found.");
        doctorRepository.save(doctor);
    }

    @Transactional
    @CacheEvict(value = "doctors", allEntries = true)
    public void remove(long doctorId) throws ServiceException {
        if (!doctorRepository.existsById(doctorId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found.");

        // Business rule: cannot delete a doctor who has Scheduled appointments
        List<Appointment> active = appointmentRepository.findByDoctorId(doctorId).stream()
            .filter(a -> "Scheduled".equals(a.getStatus()))
            .toList();
        if (!active.isEmpty())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Cannot delete: doctor has " + active.size() + " scheduled appointment(s). Cancel them first.");

        doctorRepository.deleteById(doctorId);
    }

    @Transactional(readOnly = true)
    @Cacheable("doctors")
    public List<Doctor> getAll() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "doctors", key = "#keyword")
    public List<Doctor> search(String keyword) {
        String lower = keyword.toLowerCase();
        return doctorRepository.findAll().stream()
            .filter(d -> d.getName().toLowerCase().contains(lower)
                      || d.getLicenseNumber().toLowerCase().contains(lower))
            .toList();
    }
}
