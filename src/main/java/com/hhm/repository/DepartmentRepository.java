package com.hhm.repository;

import com.hhm.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Derived queries
    Optional<Department> findByName(String name);
    boolean existsByName(String name);
}
