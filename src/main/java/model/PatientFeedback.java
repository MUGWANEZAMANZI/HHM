package model;

import java.time.LocalDateTime;

public class PatientFeedback {
    private long id;
    private long patientId;
    private String feedback;
    private String privateNotes;
    private LocalDateTime issuedAt;

    public PatientFeedback() {}

    public PatientFeedback(long patientId, String feedback, String privateNotes, LocalDateTime issuedAt) {
        this.patientId = patientId;
        this.feedback = feedback;
        this.privateNotes = privateNotes;
        this.issuedAt = issuedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getPrivateNotes() { return privateNotes; }
    public void setPrivateNotes(String privateNotes) { this.privateNotes = privateNotes; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
