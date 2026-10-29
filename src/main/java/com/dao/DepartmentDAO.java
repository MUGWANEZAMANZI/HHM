package com.dao;

import com.model.Department;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DepartmentDAO {

    private final JdbcTemplate jdbc;

    public DepartmentDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Department department) {
        jdbc.update("INSERT INTO Departments (name) VALUES (?)", department.getName());
    }

    public void update(Department department) {
        jdbc.update("UPDATE Departments SET name=? WHERE id=?",
            department.getName(), department.getId());
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM Departments WHERE id=?", id);
    }

    public List<Department> findAll() {
        return jdbc.query("SELECT * FROM Departments ORDER BY name", (rs, rowNum) -> mapRow(rs));
    }

    public Department findById(long id) {
        return findAll().stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        return d;
    }
}
