package com.graphql;

import com.model.Prescription;
import com.service.PrescriptionService;
import com.service.ServiceException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class PrescriptionResolver {

    private final PrescriptionService prescriptionService;

    public PrescriptionResolver(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @QueryMapping
    public List<Prescription> prescriptions() {
        return prescriptionService.getAll();
    }

    @QueryMapping
    public Prescription prescription(@Argument Long id) {
        return prescriptionService.getAll().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @MutationMapping
    public String issuePrescription(
            @Argument Long patientId,
            @Argument Long doctorId,
            @Argument String notes) {
        try {
            Prescription p = new Prescription(patientId, doctorId, LocalDateTime.now(), "Active", notes);
            prescriptionService.issue(p);
            return "Prescription issued successfully";
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean cancelPrescription(@Argument Long id) {
        try {
            prescriptionService.cancel(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean deletePrescription(@Argument Long id) {
        try {
            prescriptionService.remove(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
