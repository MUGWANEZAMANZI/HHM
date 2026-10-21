package model;

import java.time.LocalDateTime;

// Read-only DTO — populated by JOIN queries in AppointmentDAO, never written to DB
public class AppointmentView {
    private long id;
    private long patientId;
    private long doctorId;
    private String patientName;
    private String doctorName;
    private LocalDateTime appointmentDate;
    private String status;

    public AppointmentView() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public long getDoctorId() { return doctorId; }
    public void setDoctorId(long doctorId) { this.doctorId = doctorId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
