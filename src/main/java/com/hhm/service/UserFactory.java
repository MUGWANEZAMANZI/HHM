package com.hhm.service;


import com.hhm.dto.RegisterRequest;
import com.hhm.model.*;
import com.hhm.repository.DepartmentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserFactory {

    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    public UserFactory(PasswordEncoder passwordEncoder, DepartmentRepository departmentRepository) {
        this.passwordEncoder = passwordEncoder;
        this.departmentRepository = departmentRepository;
    }

    public User create(RegisterRequest request) {
        return switch (request.getType()) {
            case DOCTOR  -> buildDoctor(request);
            case NURSE -> buildNurse(request);
            case RECEPTIONIST -> buildReceptionist(request);
            case PATIENT -> buildPatient(request);
            case ADMIN   -> buildAdmin(request);
        };
    }

    private Receptionist buildReceptionist(RegisterRequest request) {
        Receptionist r = new Receptionist();
        fillBase(r, request);
        r.setReceptionistName(request.getName());
        return r;
    }

    private Nurse buildNurse(RegisterRequest request) {
        Nurse n = new Nurse();
        fillBase(n, request);
        n.setNurseName(request.getName());
        return n;
    }

    private Doctor buildDoctor(RegisterRequest req) {
        Doctor d = new Doctor();
        fillBase(d, req);
        d.setName(req.getName());
        d.setLicenseNumber(req.getLicenseNumber());
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Department with id " + req.getDepartmentId() + " does not exist"));
            d.setDepartment(dept);
        }
        return d;
    }

    private Patient buildPatient(RegisterRequest req) {
        Patient p = new Patient();
        fillBase(p, req);
        p.setName(req.getName());
        p.setPhone(req.getPhone());
        return p;
    }

    private Admin buildAdmin(RegisterRequest req) {
        Admin a = new Admin();
        fillBase(a, req);
        a.setName(req.getName());
        return a;
    }

    private void fillBase(User user, RegisterRequest req) {
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRegisteredAt(LocalDateTime.now());
    }
}

