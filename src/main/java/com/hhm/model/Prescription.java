package com.hhm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.ToString;

@Entity
@ToString
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    private LocalDateTime issuedAt;
    private String status;
    private String notes;

    public Prescription() {}

    public Prescription(Patient patient, Doctor doctor, LocalDateTime issuedAt, String status, String notes) {
        this.patient = patient;
        this.doctor = doctor;
        this.issuedAt = issuedAt;
        this.status = status;
        this.notes = notes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public long getPatientId() { return patient != null ? patient.getId() : 0; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public long getDoctorId() { return doctor != null ? doctor.getId() : 0; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
