package com.hhm.graphql;

import com.hhm.dto.DoctorRequest;
import com.hhm.model.Doctor;
import com.hhm.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DoctorResolver {

    private final DoctorService doctorService;

    public DoctorResolver(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @QueryMapping
    public List<Doctor> doctors(@Argument String search) {
        if (search != null && !search.isBlank())
            return doctorService.search(search);
        return doctorService.getAll();
    }

    @QueryMapping
    public Doctor doctor(@Argument Long id) {
        return doctorService.getAll().stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @MutationMapping
    public String registerDoctor(@Argument @Valid DoctorRequest input) {
        doctorService.register(input);
        return "Doctor registered successfully";
    }

    @MutationMapping
    public boolean deleteDoctor(@Argument Long id) {
        doctorService.remove(id);
        return true;
    }
}
