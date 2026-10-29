package com.service;

import com.model.Appointment;
import com.model.AppointmentView;
import com.repository.AppointmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;


    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }


    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void book(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past.");
        appointmentRepository.save(appointment);
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void reschedule(Appointment appointment) throws ServiceException {
        if (appointment.getAppointmentDate().isBefore(LocalDateTime.now()))
            throw new ServiceException("Rescheduled date cannot be in the past.");
        if (!appointmentRepository.existsById(appointment.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointment.getId() + " not found.");
        appointmentRepository.save(appointment);
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void cancel(long appointmentId) throws ServiceException {
        // Business rule: can only cancel appointments that are not already cancelled
        Appointment existing = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointmentId + " not found."));
        if ("Cancelled".equals(existing.getStatus()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Appointment is already cancelled.");
        existing.setStatus("Cancelled");
        appointmentRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void remove(long appointmentId) throws ServiceException {
        if (!appointmentRepository.existsById(appointmentId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + appointmentId + " not found.");
        appointmentRepository.deleteById(appointmentId);
    }

    // Paginated list — used by REST controller
    @Transactional(readOnly = true)
    public Page<Appointment> getAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    // Full JOIN view with patient/doctor/department names — used by GraphQL and reporting
    @Transactional(readOnly = true)
    @Cacheable("appointments")
    public List<AppointmentView> getAll() {
        return appointmentRepository.findAllWithDetails().stream()
            .map(this::mapToView)
            .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "appointments", key = "#status")
    public List<Appointment> findByStatus(String status) {
        return appointmentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "appointments", key = "#keyword")
    public List<Appointment> search(String keyword) {
        String lower = keyword.toLowerCase();
        return appointmentRepository.findAll().stream()
            .filter(a -> a.getStatus().toLowerCase().contains(lower))
            .toList();
    }

    private AppointmentView mapToView(Object[] row) {
        AppointmentView v = new AppointmentView();
        v.setId(((Number) row[0]).longValue());
        v.setPatientId(((Number) row[1]).longValue());
        v.setDoctorId(((Number) row[2]).longValue());
        v.setAppointmentDate(((java.sql.Timestamp) row[3]).toLocalDateTime());
        v.setStatus((String) row[4]);
        v.setPatientName((String) row[5]);
        v.setPatientPhone((String) row[6]);
        v.setDoctorName((String) row[7]);
        v.setDepartmentName((String) row[8]);
        return v;
    }
}
