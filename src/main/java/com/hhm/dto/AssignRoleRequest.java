package com.hhm.dto;

import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {

    @NotNull(message = "Role is required")
    private Type role;

    // Required when role = PATIENT
    private String phone;

    // Required when role = DOCTOR
    private String licenseNumber;

    // Optional when role = DOCTOR
    private Long departmentId;

    public Type getRole() { return role; }
    public void setRole(Type role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
