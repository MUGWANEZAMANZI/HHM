package com.model;

import java.time.LocalDateTime;

public class Appointment {
    private long id;
    private long patientId;
    private long doctorId;
    private LocalDateTime appointmentDate;
    private String status;

    public Appointment() {}

    public Appointment(long patientId, long doctorId, LocalDateTime appointmentDate, String status) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public long getDoctorId() { return doctorId; }
    public void setDoctorId(long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
