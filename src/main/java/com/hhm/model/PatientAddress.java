package com.hhm.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.ToString;

@Entity
@ToString
public class PatientAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long patientId;
    private String district;
    private String sector;
    private String cell;
    private String village;

    public PatientAddress() {}

    public PatientAddress(long patientId, String district, String sector, String cell, String village) {
        this.patientId = patientId;
        this.district = district;
        this.sector = sector;
        this.cell = cell;
        this.village = village;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPatientId() { return patientId; }
    public void setPatientId(long patientId) { this.patientId = patientId; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getCell() { return cell; }
    public void setCell(String cell) { this.cell = cell; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
}
