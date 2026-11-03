package com.hhm.service;

import com.hhm.model.Patient;
import com.hhm.repository.DoctorRepository;
import com.hhm.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Demonstrates @Transactional rollback:
 *
 *  Step 1 — save the new patient  ──► written to DB (but NOT committed yet)
 *  Step 2 — validate the doctor   ──► throws if doctor doesn't exist
 *  Step 3 — book the appointment  ──► written to DB (but NOT committed yet)
 *
 *  If step 2 or 3 throws, Spring rolls back the whole unit of work.
 *  The patient from step 1 is erased — the database is left exactly as it was before.
 */
@Service
public class PatientAdmissionService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentService appointmentService;

    public PatientAdmissionService(PatientRepository patientRepository,
                                   DoctorRepository doctorRepository,
                                   AppointmentService appointmentService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentService = appointmentService;
    }

    /**
     * Registers a patient and books their first appointment atomically.
     *
     * @return the new patient's ID
     * @throws ServiceException if the doctor is not found or the date is in the past —
     *         the patient row saved earlier is automatically rolled back
     */
    @Transactional
    public long admitPatient(String name, String phone, String email,
                             long doctorId, LocalDateTime appointmentDate) {

        // ── STEP 1: save the new patient ─────────────────────────────────────
        Patient patient = new Patient(name, phone);
        patient.setEmail(email);
        patientRepository.save(patient);

        // ── STEP 2: validate the doctor exists ───────────────────────────────
        if (!doctorRepository.existsById(doctorId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Doctor with id " + doctorId + " not found. Admission cancelled — no records were saved.");

        // ── STEP 3: book the first appointment ───────────────────────────────
        if (appointmentDate.isBefore(LocalDateTime.now()))
            throw new ServiceException("Appointment date cannot be in the past. Admission cancelled.");

        appointmentService.book(patient.getId(), doctorId, appointmentDate, "Scheduled");

        // ── COMMIT ───────────────────────────────────────────────────────────
        return patient.getId();
    }
}
