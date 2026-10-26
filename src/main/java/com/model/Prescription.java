package com.model;

import java.time.LocalDateTime;

public class Prescription {
    private long id;
    private long patientId;
    private long doctorId;
    private LocalDateTime issuedAt;
    private String status;
    private String notes;

    public Prescription() {}

    public Prescription(long patientId, long doctorId, LocalDateTime issuedAt, String status, String notes) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.issuedAt = issuedAt;
        this.status = status;
        this.notes = notes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public long getDoctorId() { return doctorId; }
    public void setDoctorId(long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
