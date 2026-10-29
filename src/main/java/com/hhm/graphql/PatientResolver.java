package com.hhm.graphql;

import com.hhm.model.Patient;
import com.hhm.service.PatientService;
import com.hhm.service.ServiceException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PatientResolver {

    private final PatientService patientService;

    public PatientResolver(PatientService patientService) {
        this.patientService = patientService;
    }

    @QueryMapping
    public List<Patient> patients(@Argument String search) {
        if (search != null && !search.isBlank())
            return patientService.search(search);
        return patientService.getAll();
    }

    @QueryMapping
    public Patient patient(@Argument Long id) {
        return patientService.getAll().stream()
                .filter(p -> id != null && p.getId() == id.longValue())
                .findFirst()
                .orElse(null);
    }

    @MutationMapping
    public String registerPatient(
            @Argument String name,
            @Argument String phone,
            @Argument String email,
            @Argument String address,
            @Argument String district,
            @Argument String sector,
            @Argument String cell,
            @Argument String village) {
        try {
            long newId = patientService.register(name, phone, email, address, district, sector, cell, village);
            return "Patient registered with id " + newId;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean deletePatient(@Argument Long id) {
        try {
            patientService.remove(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
