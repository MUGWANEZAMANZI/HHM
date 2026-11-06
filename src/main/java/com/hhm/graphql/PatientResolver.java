package com.hhm.graphql;

import com.hhm.dto.PatientPage;
import com.hhm.dto.PatientRequest;
import com.hhm.model.Patient;
import com.hhm.service.PatientService;
import com.hhm.service.ServiceException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return patientService.getAll(Pageable.unpaged()).getContent();
    }

    @QueryMapping
    public PatientPage patientsPaged(@Argument Integer page, @Argument Integer size) {
        int p = (page != null && page >= 0) ? page : 0;
        int s = (size != null && size > 0) ? size : 10;
        Page<Patient> result = patientService.getAll(PageRequest.of(p, s));
        return new PatientPage(result);
    }

    @QueryMapping
    public Patient patient(@Argument Long id) {
        if (id == null) return null;
        try {
            return patientService.findById(id);
        } catch (ServiceException e) {
            return null;
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public String registerPatient(@Argument @Valid PatientRequest input) {
        long newId = patientService.register(
                input.getName(), input.getPhone(), input.getEmail(),
                input.getAddress(), input.getDistrict(), input.getSector(),
                input.getCell(), input.getVillage());
        return "Patient registered with id " + newId;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public boolean deletePatient(@Argument Long id) {
        patientService.remove(id);
        return true;
    }
}
