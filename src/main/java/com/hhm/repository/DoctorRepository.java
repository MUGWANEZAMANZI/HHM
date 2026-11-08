package com.hhm.repository;

import com.hhm.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Derived queries
    List<Doctor> findByDepartmentId(long departmentId);
    List<Doctor> findByNameContainingIgnoreCase(String name);
    List<Doctor> findByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    // DB-level keyword search across name and license number — avoids loading all rows.
    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Doctor> search(@Param("keyword") String keyword);

    // Pagination
    Page<Doctor> findAll(Pageable pageable);
}
