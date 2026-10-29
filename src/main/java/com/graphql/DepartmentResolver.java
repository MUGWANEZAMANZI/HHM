package com.graphql;

import com.dao.DepartmentDAO;
import com.model.Department;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DepartmentResolver {

    private final DepartmentDAO departmentDAO;

    public DepartmentResolver(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    @QueryMapping
    public List<Department> departments() {
        return departmentDAO.findAll();
    }
}
