package com.hhm.service;

import com.hhm.dto.DepartmentRequest;
import com.hhm.model.Department;
import com.hhm.repository.DepartmentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    @CacheEvict(value = "departments",allEntries = true)
    public Department create(DepartmentRequest req) throws ServiceException {
        if (departmentRepository.existsByName(req.getName()))
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "Department \"" + req.getName() + "\" already exists.");
        return departmentRepository.save(new Department(req.getName()));
    }

    @Transactional
    public void remove(long id) throws ServiceException {
        if (!departmentRepository.existsById(id))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Department with id " + id + " not found.");
        departmentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "'allDepartments'")
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "'departmentById:' + #id")
    public Department findById(long id) throws ServiceException {
        return departmentRepository.findById(id)
            .orElseThrow(() -> new ServiceException(ServiceException.Type.NOT_FOUND,
                "Department with id " + id + " not found."));
    }
}
