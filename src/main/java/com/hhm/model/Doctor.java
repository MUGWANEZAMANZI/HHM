package com.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.ToString;

@Entity
@ToString
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private long departmentId;
    private String licenseNumber;

    public Doctor() {}

    public Doctor(String name, long departmentId, String licenseNumber) {
        this.name = name;
        this.departmentId = departmentId;
        this.licenseNumber = licenseNumber;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getDepartmentId() { return departmentId; }
    public void setDepartmentId(long departmentId) { this.departmentId = departmentId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
