package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
@DiscriminatorValue("DOCTOR")
public class Doctor extends User{
    private String name;
    private String licenseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public Doctor() {
        super();
    }

    public Doctor(String name, Department department, String licenseNumber) {
        super();
        this.name = name;
        this.department = department;
        this.licenseNumber = licenseNumber;
    }

    public long getId() { return super.getId(); }
    public void setId(long id) { super.setId(id); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public long getDepartmentId() { return department != null ? department.getId() : 0; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
