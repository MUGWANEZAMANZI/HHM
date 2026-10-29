package com.hhm.repository;

import com.hhm.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Derived queries
    List<Doctor> findByDepartmentId(long departmentId);
    List<Doctor> findByNameContainingIgnoreCase(String name);
    List<Doctor> findByLicenseNumber(String licenseNumber);

    // Pagination
    Page<Doctor> findAll(Pageable pageable);
}
