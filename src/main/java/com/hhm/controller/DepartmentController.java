package com.hhm.controller;

import com.hhm.dto.ApiResult;
import com.hhm.dto.DepartmentRequest;
import com.hhm.model.Department;
import com.hhm.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Department", description = "Operations for managing hospital departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "List all departments")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Departments returned successfully")
    })
    public ResponseEntity<ApiResult<List<Department>>> getAllDepartments() {
        List<Department> result = departmentService.getAll();
        return ResponseEntity.ok(ApiResult.success(result.size() + " department(s) found", result));
    }


    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    @Operation(
        summary = "Create a new department",
        description = "Creates a new department. Name must be unique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Department created successfully"),
        @ApiResponse(responseCode = "409", description = "Department name already exists"),
        @ApiResponse(responseCode = "422", description = "Validation failed — see message field")
    })
    public ResponseEntity<ApiResult<Department>> createDepartment(@Valid @RequestBody DepartmentRequest req) {
        Department created = departmentService.create(req);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Department \"" + created.getName() + "\" created with id " + created.getId(), created));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a department",
        description = "Permanently removes a department. Fails if doctors are still assigned to it."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Department deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<Void> deleteDepartment(
            @Parameter(description = "ID of the department to delete", example = "1")
            @PathVariable long id) {
        departmentService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
