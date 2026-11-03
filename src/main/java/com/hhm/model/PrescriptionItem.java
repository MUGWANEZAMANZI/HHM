package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id")
    private MedicalInventory medication;

    private String dosage;
    private String frequency;
    private int quantity;
    private int refillsAllowed;

    public PrescriptionItem() {}

    public PrescriptionItem(Prescription prescription, MedicalInventory medication,
                            String dosage, String frequency, int quantity, int refillsAllowed) {
        this.prescription = prescription;
        this.medication = medication;
        this.dosage = dosage;
        this.frequency = frequency;
        this.quantity = quantity;
        this.refillsAllowed = refillsAllowed;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }
    public long getPrescriptionId() { return prescription != null ? prescription.getId() : 0; }

    public MedicalInventory getMedication() { return medication; }
    public void setMedication(MedicalInventory medication) { this.medication = medication; }
    public long getMedicationId() { return medication != null ? medication.getId() : 0; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRefillsAllowed() { return refillsAllowed; }
    public void setRefillsAllowed(int refillsAllowed) { this.refillsAllowed = refillsAllowed; }
}
