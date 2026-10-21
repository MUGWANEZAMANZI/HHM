package controller;

import dao.PatientAddressDAO;
import dao.PatientDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Patient;
import model.PatientAddress;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PatientController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private TextField districtField;
    @FXML private TextField sectorField;
    @FXML private TextField cellField;
    @FXML private TextField villageField;
    @FXML private TextField searchField;
    @FXML private Button saveBtn;
    @FXML private Label statusLabel;

    @FXML private TableView<PatientRow> patientTable;
    @FXML private TableColumn<PatientRow, String> colId;
    @FXML private TableColumn<PatientRow, String> colName;
    @FXML private TableColumn<PatientRow, String> colPhone;
    @FXML private TableColumn<PatientRow, String> colEmail;
    @FXML private TableColumn<PatientRow, String> colAddress;
    @FXML private TableColumn<PatientRow, String> colDistrict;
    @FXML private TableColumn<PatientRow, String> colSector;
    @FXML private TableColumn<PatientRow, String> colCell;
    @FXML private TableColumn<PatientRow, String> colVillage;

    private final PatientDAO patientDAO = new PatientDAO();
    private final PatientAddressDAO addressDAO = new PatientAddressDAO();
    private long selectedPatientId = -1;

    @FXML
    public void initialize() {
        setupColumns();
        loadAll();
        patientTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> { if (row != null) populateForm(row); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        try {
            Patient p = new Patient(
                nameField.getText().trim(),
                phoneField.getText().trim(),
                emailField.getText().trim()
            );
            p.setAddress(addressField.getText().trim());

            if (selectedPatientId == -1) {
                long newId = insertAndGetId(p);
                if (hasAddressDetail()) addressDAO.insert(buildAddress(newId));
                setStatus("Patient saved successfully.");
            } else {
                p.setId(selectedPatientId);
                patientDAO.update(p);
                if (hasAddressDetail()) {
                    PatientAddress existing = addressDAO.findByPatient(selectedPatientId);
                    if (existing == null) addressDAO.insert(buildAddress(selectedPatientId));
                    else addressDAO.update(buildAddress(selectedPatientId));
                }
                setStatus("Patient updated successfully.");
            }
            onClear();
            loadAll();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    void onDelete() {
        PatientRow row = patientTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select a patient to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete patient \"" + row.name + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    addressDAO.deleteByPatient(row.id);
                    patientDAO.delete(row.id);
                    onClear();
                    loadAll();
                    setStatus("Patient deleted.");
                } catch (SQLException e) {
                    setStatus("Error: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) { loadAll(); return; }
        try {
            patientTable.setItems(FXCollections.observableArrayList(
                toRows(patientDAO.search(keyword))));
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    void onShowAll() {
        searchField.clear();
        loadAll();
    }

    @FXML
    void onClear() {
        nameField.clear(); phoneField.clear();
        emailField.clear(); addressField.clear();
        districtField.clear(); sectorField.clear();
        cellField.clear(); villageField.clear();
        selectedPatientId = -1;
        saveBtn.setText("Save Patient");
        statusLabel.setText("");
        patientTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        String name  = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();

        if (name == null || name.trim().isEmpty()) {
            setStatus("Name is required."); return false;
        }
        if (phone == null || phone.trim().isEmpty()) {
            setStatus("Phone number is required."); return false;
        }
        if (email == null || email.trim().isEmpty()) {
            setStatus("Email is required."); return false;
        }
        if (!email.contains("@")) {
            setStatus("Email is not valid."); return false;
        }
        return true;
    }

    private void setupColumns() {
        colId.setCellValueFactory(c      -> new SimpleStringProperty(String.valueOf(c.getValue().id)));
        colName.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().name));
        colPhone.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().phone));
        colEmail.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().email));
        colAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().address));
        colDistrict.setCellValueFactory(c-> new SimpleStringProperty(c.getValue().district));
        colSector.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().sector));
        colCell.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().cell));
        colVillage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().village));
    }

    private void loadAll() {
        try {
            patientTable.setItems(FXCollections.observableArrayList(
                toRows(patientDAO.findAll())));
        } catch (SQLException e) {
            setStatus("Error loading patients: " + e.getMessage());
        }
    }

    private List<PatientRow> toRows(List<Patient> patients) throws SQLException {
        List<PatientRow> rows = new ArrayList<>();
        for (Patient p : patients) {
            PatientAddress addr = addressDAO.findByPatient(p.getId());
            rows.add(new PatientRow(p, addr));
        }
        return rows;
    }

    private void populateForm(PatientRow row) {
        selectedPatientId = row.id;
        nameField.setText(row.name);
        phoneField.setText(row.phone);
        emailField.setText(row.email);
        addressField.setText(row.address);
        districtField.setText(row.district);
        sectorField.setText(row.sector);
        cellField.setText(row.cell);
        villageField.setText(row.village);
        saveBtn.setText("Update Patient");
        setStatus("Editing patient #" + row.id);
    }

    private long insertAndGetId(Patient p) throws SQLException {
        String sql = "INSERT INTO Patient (Name, Address, Phone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getName());
            stmt.setString(2, p.getAddress());
            stmt.setString(3, p.getPhone());
            stmt.setString(4, p.getEmail());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getLong(1);
        }
        throw new SQLException("Failed to retrieve generated patient ID.");
    }

    private boolean hasAddressDetail() {
        return !districtField.getText().isBlank() || !sectorField.getText().isBlank()
            || !cellField.getText().isBlank()     || !villageField.getText().isBlank();
    }

    private PatientAddress buildAddress(long patientId) {
        return new PatientAddress(patientId,
            districtField.getText().trim(),
            sectorField.getText().trim(),
            cellField.getText().trim(),
            villageField.getText().trim());
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    // -------------------------------------------------------------------------
    // Flat view row — joins Patient + PatientAddress for TableView display
    // -------------------------------------------------------------------------
    public static class PatientRow {
        final long id;
        final String name, phone, email, address, district, sector, cell, village;

        PatientRow(Patient p, PatientAddress a) {
            id       = p.getId();
            name     = nvl(p.getName());
            phone    = nvl(p.getPhone());
            email    = nvl(p.getEmail());
            address  = nvl(p.getAddress());
            district = a != null ? nvl(a.getDistrict()) : "";
            sector   = a != null ? nvl(a.getSector())   : "";
            cell     = a != null ? nvl(a.getCell())     : "";
            village  = a != null ? nvl(a.getVillage())  : "";
        }

        private static String nvl(String s) { return s != null ? s : ""; }
    }
}
