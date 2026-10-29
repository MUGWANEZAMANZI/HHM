package com.graphql;

import com.dto.DoctorRequest;
import com.model.Doctor;
import com.service.DoctorService;
import com.service.ServiceException;
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
    public String registerDoctor(
            @Argument String name,
            @Argument Long departmentId,
            @Argument String licenseNumber) {
        try {
            DoctorRequest req = new DoctorRequest();
            req.setName(name);
            req.setDepartmentId(departmentId);
            req.setLicenseNumber(licenseNumber);
            doctorService.register(req);
            return "Doctor registered successfully";
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean deleteDoctor(@Argument Long id) {
        try {
            doctorService.remove(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
