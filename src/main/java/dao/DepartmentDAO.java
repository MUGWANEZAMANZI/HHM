package dao;

import model.Department;
import util.AppCache;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public void insert(Department department) throws SQLException {
        String sql = "INSERT INTO Departments (name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, department.getName());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Department.class);
    }

    public List<Department> findAll() throws SQLException {
        List<Department> cached = AppCache.getAll(Department.class);
        if (cached != null) return cached;

        List<Department> list = new ArrayList<>();
        String sql = "SELECT * FROM Departments ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Department d = new Department();
                d.setId(rs.getLong("id"));
                d.setName(rs.getString("name"));
                list.add(d);
            }
        }
        AppCache.put(Department.class, list);
        return list;
    }

    public Department findById(long id) throws SQLException {
        return findAll().stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    public void update(Department department) throws SQLException {
        String sql = "UPDATE Departments SET name=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, department.getName());
            stmt.setLong(2, department.getId());
            stmt.executeUpdate();
        }
        AppCache.invalidate(Department.class);
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM Departments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
        AppCache.invalidate(Department.class);
    }
}
