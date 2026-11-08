package com.hhm.service;

import com.hhm.dto.DoctorRequest;
import com.hhm.model.Appointment;
import com.hhm.model.Department;
import com.hhm.model.Doctor;
import com.hhm.repository.AppointmentRepository;
import com.hhm.repository.DepartmentRepository;
import com.hhm.repository.DoctorRepository;
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

        // existsByLicenseNumberIgnoreCase → single COUNT query — O(1) with index,
        // replacing the previous findAll().stream().anyMatch() full table scan.
        if (doctorRepository.existsByLicenseNumberIgnoreCase(req.getLicenseNumber()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "License number \"" + req.getLicenseNumber() + "\" is already registered.");

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                        "Department with id " + req.getDepartmentId() + " not found."));

        doctorRepository.save(new Doctor(req.getName(), department, req.getLicenseNumber()));
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

        List<Appointment> active = appointmentRepository.findByDoctor_Id(doctorId).stream()
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
        // DB-level LIKE filter — only matching rows are transferred over the wire.
        // Replaces the previous findAll() + in-memory stream scan (O(n) full load).
        return doctorRepository.search(keyword);
    }

    @Transactional
    public Doctor findById(long doctorId) throws ServiceException {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                        "Doctor with id " + doctorId + " not found."));
    }
}
