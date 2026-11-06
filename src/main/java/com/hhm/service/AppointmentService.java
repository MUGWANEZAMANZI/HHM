package com.hhm.service;

import com.hhm.model.Appointment;
import com.hhm.model.AppointmentView;
import com.hhm.model.Doctor;
import com.hhm.model.Patient;
import com.hhm.repository.AppointmentRepository;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void book(long patientId, long doctorId, LocalDateTime date, String status) throws ServiceException {
        if (date.isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past.");
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found."));
        appointmentRepository.save(new Appointment(patient, doctor, date, status));
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void reschedule(long id, long patientId, long doctorId, LocalDateTime date) throws ServiceException {
        if (date.isBefore(LocalDateTime.now()))
            throw new ServiceException("Rescheduled date cannot be in the past.");
        Appointment existing = appointmentRepository.findById(id)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + id + " not found."));
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Patient with id " + patientId + " not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found."));
        existing.setPatient(patient);
        existing.setDoctor(doctor);
        existing.setAppointmentDate(date);
        existing.setStatus("Rescheduled");
        appointmentRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void cancel(long appointmentId) throws ServiceException {
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

    // Single source of truth: a plain-POJO list with all JOIN data embedded.
    // AppointmentView has no Hibernate proxies, so Jackson can round-trip it safely.
    @Transactional(readOnly = true)
    @Cacheable("appointments")
    public List<AppointmentView> getAll() {
        return appointmentRepository.findAllWithDetails().stream()
            .map(this::mapToView)
            .collect(java.util.stream.Collectors.toList());
    }

    // In-memory filter on the already-cached list — no DB call, no separate cache entry.
    // Returns AppointmentView (not Appointment) so LAZY fields are never involved.
    public List<AppointmentView> findByStatus(String status) {
        return getAll().stream()
            .filter(v -> status.equalsIgnoreCase(v.getStatus()))
            .toList();
    }

    // In-memory search on the cached list across patient name, doctor name, and status.
    public List<AppointmentView> search(String keyword) {
        String lower = keyword.toLowerCase();
        return getAll().stream()
            .filter(v -> contains(v.getPatientName(), lower)
                      || contains(v.getDoctorName(),  lower)
                      || contains(v.getStatus(),       lower))
            .toList();
    }

    private static boolean contains(String field, String lower) {
        return field != null && field.toLowerCase().contains(lower);
    }

    private AppointmentView mapToView(Object[] row) {
        AppointmentView v = new AppointmentView();
        v.setId(((Number) row[0]).longValue());
        v.setPatientId(((Number) row[1]).longValue());
        v.setDoctorId(((Number) row[2]).longValue());
        Object dateCell = row[3];
        v.setAppointmentDate(dateCell instanceof java.sql.Timestamp ts
                ? ts.toLocalDateTime()
                : (java.time.LocalDateTime) dateCell);
        v.setStatus((String) row[4]);
        v.setPatientName((String) row[5]);
        v.setPatientPhone((String) row[6]);
        v.setDoctorName((String) row[7]);
        v.setDepartmentName((String) row[8]);
        return v;
    }
}
