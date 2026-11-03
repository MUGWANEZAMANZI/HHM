package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
public class PatientAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private String district;
    private String sector;
    private String cell;
    private String village;

    public PatientAddress() {}

    public PatientAddress(Patient patient, String district, String sector, String cell, String village) {
        this.patient = patient;
        this.district = district;
        this.sector = sector;
        this.cell = cell;
        this.village = village;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public long getPatientId() { return patient != null ? patient.getId() : 0; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getCell() { return cell; }
    public void setCell(String cell) { this.cell = cell; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
}
