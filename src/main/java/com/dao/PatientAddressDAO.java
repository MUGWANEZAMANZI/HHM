package com.dao;

import com.model.PatientAddress;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PatientAddressDAO {

    private final JdbcTemplate jdbc;

    public PatientAddressDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(PatientAddress a) {
        jdbc.update(
            "INSERT INTO `Patient Address` (patientId, District, Sector, Cell, Village) VALUES (?, ?, ?, ?, ?)",
            a.getPatientId(), a.getDistrict(), a.getSector(), a.getCell(), a.getVillage());
    }

    public void update(PatientAddress a) {
        jdbc.update(
            "UPDATE `Patient Address` SET District=?, Sector=?, Cell=?, Village=? WHERE patientId=?",
            a.getDistrict(), a.getSector(), a.getCell(), a.getVillage(), a.getPatientId());
    }

    public void deleteByPatient(long patientId) {
        jdbc.update("DELETE FROM `Patient Address` WHERE patientId=?", patientId);
    }

    public PatientAddress findByPatient(long patientId) {
        return findAllList().stream()
            .filter(a -> a.getPatientId() == patientId).findFirst().orElse(null);
    }

    public Map<Long, PatientAddress> findAll() {
        Map<Long, PatientAddress> map = new HashMap<>();
        for (PatientAddress a : findAllList()) map.put(a.getPatientId(), a);
        return map;
    }

    private List<PatientAddress> findAllList() {
        return jdbc.query("SELECT * FROM `Patient Address`", (rs, rowNum) -> mapRow(rs));
    }

    private PatientAddress mapRow(ResultSet rs) throws SQLException {
        PatientAddress a = new PatientAddress();
        a.setId(rs.getLong("Id"));
        a.setPatientId(rs.getLong("patientId"));
        a.setDistrict(rs.getString("District"));
        a.setSector(rs.getString("Sector"));
        a.setCell(rs.getString("Cell"));
        a.setVillage(rs.getString("Village"));
        return a;
    }
}
