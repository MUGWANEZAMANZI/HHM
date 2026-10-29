package com.hhm.graphql;

import com.hhm.repository.DepartmentRepository;
import com.hhm.model.Department;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DepartmentResolver {

    private final DepartmentRepository departmentRepository;

    public DepartmentResolver(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @QueryMapping
    public List<Department> departments() {
        return departmentRepository.findAll();
    }
}
