package com.hhm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.ToString;

@Entity
@ToString
public class PatientFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private String feedback;
    private String privateNotes;
    private LocalDateTime issuedAt;

    public PatientFeedback() {}

    public PatientFeedback(Patient patient, String feedback, String privateNotes, LocalDateTime issuedAt) {
        this.patient = patient;
        this.feedback = feedback;
        this.privateNotes = privateNotes;
        this.issuedAt = issuedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public long getPatientId() { return patient != null ? patient.getId() : 0; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
