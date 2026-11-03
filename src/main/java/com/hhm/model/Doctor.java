package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String licenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public Doctor() {}

    public Doctor(String name, Department department, String licenseNumber) {
        this.name = name;
        this.department = department;
        this.licenseNumber = licenseNumber;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public long getDepartmentId() { return department != null ? department.getId() : 0; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
