package com.hhm.graphql;

import com.hhm.dto.PrescriptionRequest;
import com.hhm.model.Prescription;
import com.hhm.service.PrescriptionService;
import jakarta.validation.Valid;
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
    public String issuePrescription(@Argument @Valid PrescriptionRequest input) {
        Prescription p = new Prescription(
                input.getPatientId(), input.getDoctorId(), LocalDateTime.now(), "Active", input.getNotes());
        prescriptionService.issue(p);
        return "Prescription issued successfully";
    }

    @MutationMapping
    public boolean cancelPrescription(@Argument Long id) {
        prescriptionService.cancel(id);
        return true;
    }

    @MutationMapping
    public boolean deletePrescription(@Argument Long id) {
        prescriptionService.remove(id);
        return true;
    }
}
