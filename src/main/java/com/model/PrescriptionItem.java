package com.model;

public class PrescriptionItem {
    private long id;
    private long prescriptionId;
    private long medicationId;
    private String dosage;
    private String frequency;
    private int quantity;
    private int refillsAllowed;

    public PrescriptionItem() {}

    public PrescriptionItem(long prescriptionId, long medicationId, String dosage, String frequency, int quantity, int refillsAllowed) {
        this.prescriptionId = prescriptionId;
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.frequency = frequency;
        this.quantity = quantity;
        this.refillsAllowed = refillsAllowed;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(long prescriptionId) { this.prescriptionId = prescriptionId; }

    public long getMedicationId() { return medicationId; }
    public void setMedicationId(long medicationId) { this.medicationId = medicationId; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRefillsAllowed() { return refillsAllowed; }
    public void setRefillsAllowed(int refillsAllowed) { this.refillsAllowed = refillsAllowed; }
}
