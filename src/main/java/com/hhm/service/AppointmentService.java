package com.hhm.service;

import com.hhm.model.Appointment;
import com.hhm.model.AppointmentView;
import com.hhm.model.Doctor;
import com.hhm.model.Patient;
import com.hhm.repository.AppointmentRepository;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final Executor dbLookupExecutor;
    private final ConcurrentHashMap<String, Boolean> inflightSlots = new ConcurrentHashMap<>();


    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              @Qualifier("dbLookupExecutor") Executor dbLookupExecutor) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.dbLookupExecutor = dbLookupExecutor;
    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void book(long patientId, long doctorId, LocalDateTime date, String status) throws ServiceException {
        if (date.isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past.");

        String slot = doctorId + ":" + date;
        if (inflightSlots.putIfAbsent(slot, Boolean.TRUE) != null)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                    "A booking for this slot is already in progress.");


        try {
            if (appointmentRepository.existsActiveBooking(doctorId, date))
                throw new ServiceException(ServiceException.Type.CONFLICT,
                        "Doctor already has an appointment at this time.");
            // Fetch patient and doctor in parallel — these are independent DB calls.
            // Both run on dbLookupExecutor threads while the calling thread waits at join().
            CompletableFuture<Patient> patientFuture = CompletableFuture.supplyAsync(
                    () -> patientRepository.findById(patientId)
                            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                                    "Patient with id " + patientId + " not found.")),
                    dbLookupExecutor
            );
            CompletableFuture<Doctor> doctorFuture = CompletableFuture.supplyAsync(
                    () -> doctorRepository.findById(doctorId)
                            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                                    "Doctor with id " + doctorId + " not found.")),
                    dbLookupExecutor
            );

            // Block until both lookups finish, then proceed with the save.
            CompletableFuture.allOf(patientFuture, doctorFuture).join();
            appointmentRepository.save(new Appointment(patientFuture.join(), doctorFuture.join(), date, status));
        } finally {
            inflightSlots.remove(slot);  // always release the slot after the transaction commits or rolls back
        }

    }

    @Transactional
    @CacheEvict(value = "appointments", allEntries = true)
    public void reschedule(long id, long patientId, long doctorId, LocalDateTime date) throws ServiceException {
        if (date.isBefore(LocalDateTime.now()))
            throw new ServiceException("Rescheduled date cannot be in the past.");

        // The existing appointment lookup must come first (sequential — we need it to exist).
        Appointment existing = appointmentRepository.findById(id)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Appointment with id " + id + " not found."));

        // Patient and doctor lookups are independent — run them in parallel.
        CompletableFuture<Patient> patientFuture = CompletableFuture.supplyAsync(
            () -> patientRepository.findById(patientId)
                .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                    "Patient with id " + patientId + " not found.")),
            dbLookupExecutor
        );
        CompletableFuture<Doctor> doctorFuture = CompletableFuture.supplyAsync(
            () -> doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                    "Doctor with id " + doctorId + " not found.")),
            dbLookupExecutor
        );

        CompletableFuture.allOf(patientFuture, doctorFuture).join();
        existing.setPatient(patientFuture.join());
        existing.setDoctor(doctorFuture.join());
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
    // parallelStream(): mapToView is a pure function (no shared state, no I/O) — safe
    // to execute across multiple threads. Speeds up the mapping step under large datasets.
    @Transactional(readOnly = true)
    @Cacheable("appointments")
    public List<AppointmentView> getAll() {
        return appointmentRepository.findAllWithDetails().parallelStream()
            .map(this::mapToView)
            .collect(java.util.stream.Collectors.toList());
    }

    // parallelStream(): filters an in-memory cached list — no DB call.
    // Safe because AppointmentView is immutable after construction.
    public List<AppointmentView> findByStatus(String status) {
        return getAll().parallelStream()
            .filter(v -> status.equalsIgnoreCase(v.getStatus()))
            .toList();
    }

    // parallelStream(): same rationale — pure predicate, immutable source, no side effects.
    public List<AppointmentView> search(String keyword) {
        String lower = keyword.toLowerCase();
        return getAll().parallelStream()
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
