package com.hhm.graphql;

import com.hhm.dto.DepartmentRequest;
import com.hhm.model.Department;
import com.hhm.service.DepartmentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DepartmentResolver {

    private final DepartmentService departmentService;

    public DepartmentResolver(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @QueryMapping
    public List<Department> departments() {
        return departmentService.getAll();
    }

    @QueryMapping
    public Department department(@Argument Long id) {
        return departmentService.findById(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public String createDepartment(@Argument String name) {
        DepartmentRequest req = new DepartmentRequest();
        req.setName(name);
        Department saved = departmentService.create(req);
        return "Department \"" + saved.getName() + "\" created with id " + saved.getId();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @MutationMapping
    public boolean deleteDepartment(@Argument Long id) {
        departmentService.remove(id);
        return true;
    }
}
