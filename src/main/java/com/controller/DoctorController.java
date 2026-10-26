package com.controller;

import com.dto.DoctorRequest;
import com.model.Doctor;
import com.service.DoctorService;
import com.service.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Doctor", description = "Operations for managing doctor records")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    @Operation(summary = "List all doctors", description = "Returns all doctors. Pass ?search= to filter by name or license number.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Doctors returned successfully")
    })
    public ResponseEntity<List<Doctor>> getAllDoctors(
            @Parameter(description = "Optional keyword — filters by name or license number")
            @RequestParam(required = false) String search) {
        List<Doctor> result = (search != null && !search.isBlank())
                ? doctorService.search(search)
                : doctorService.getAll();
        return ResponseEntity.ok(result);
    }

   @PostMapping
   @Operation(summary = "Register a new doctor", description = "Creates a doctor record. License number must be unique and department must exist.")
   @ApiResponses({
       @ApiResponse(responseCode = "201", description = "Doctor registered successfully"),
       @ApiResponse(responseCode = "409", description = "License number is already taken"),
       @ApiResponse(responseCode = "404", description = "Department ID does not exist"),
       @ApiResponse(responseCode = "422", description = "Validation failed — missing or invalid fields"),
       @ApiResponse(responseCode = "500", description = "Unexpected server error")
   })
   public ResponseEntity<String> createDoctor(@Valid @RequestBody DoctorRequest req) {
       try {
           doctorService.register(req);
           return ResponseEntity.status(201).body("Doctor registered successfully");
       } catch (ServiceException e) {
           return switch (e.getType()) {
               case CONFLICT  -> ResponseEntity.status(409).body(e.getMessage());
               case NOT_FOUND -> ResponseEntity.status(404).body(e.getMessage());
               default        -> ResponseEntity.status(422).body(e.getMessage());
           };
       }
   }

    @PutMapping("/{id}")
    @Operation(summary = "Update a doctor", description = "Updates name, departmentId, and licenseNumber for the given doctor id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Doctor updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed or doctor not found")
    })
    public ResponseEntity<String> updateDoctor(@PathVariable long id, @Valid @RequestBody DoctorRequest req) {
        try {
            Doctor doctor = new Doctor(req.getName(), req.getDepartmentId(), req.getLicenseNumber());
            doctor.setId(id);
            doctorService.update(doctor);
            return ResponseEntity.ok("Doctor updated successfully");
        } catch (ServiceException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a doctor", description = "Removes the doctor. Fails if they have scheduled appointments.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Doctor deleted — no content returned"),
        @ApiResponse(responseCode = "409", description = "Doctor has active scheduled appointments")
    })
    public ResponseEntity<String> deleteDoctor(
            @Parameter(description = "ID of the doctor to delete", example = "1")
            @PathVariable long id) {
        try {
            doctorService.remove(id);
            return ResponseEntity.noContent().build();
        } catch (ServiceException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }


}
