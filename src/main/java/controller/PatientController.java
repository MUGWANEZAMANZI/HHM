package controller;

import dao.PatientAddressDAO;
import dao.PatientDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Patient;
import model.PatientAddress;
import service.PatientService;
import service.ServiceException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final PatientService    patientService = new PatientService();
    private final PatientDAO        patientDAO     = new PatientDAO();
    private final PatientAddressDAO addressDAO     = new PatientAddressDAO();
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
            if (selectedPatientId == -1) {
                patientService.register(
                    nameField.getText().trim(),
                    phoneField.getText().trim(),
                    emailField.getText().trim(),
                    addressField.getText().trim(),
                    districtField.getText().trim(),
                    sectorField.getText().trim(),
                    cellField.getText().trim(),
                    villageField.getText().trim()
                );
                setStatus("Patient saved successfully.");
            } else {
                Patient p = new Patient(
                    nameField.getText().trim(),
                    phoneField.getText().trim(),
                    emailField.getText().trim()
                );
                p.setId(selectedPatientId);
                p.setAddress(addressField.getText().trim());
                patientService.update(p,
                    districtField.getText().trim(),
                    sectorField.getText().trim(),
                    cellField.getText().trim(),
                    villageField.getText().trim()
                );
                setStatus("Patient updated successfully.");
            }
            onClear();
            loadAll();
        } catch (ServiceException e) {
            setStatus(e.getMessage());
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
                    patientService.remove(row.id);
                    onClear();
                    loadAll();
                    setStatus("Patient deleted.");
                } catch (ServiceException e) { setStatus(e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) { loadAll(); return; }
        try {
            patientTable.setItems(FXCollections.observableArrayList(
                toRows(patientService.search(keyword))));
        } catch (ServiceException | SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML void onShowAll() { searchField.clear(); loadAll(); }

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
        if (nameField.getText().isBlank())  { setStatus("Name is required."); return false; }
        if (phoneField.getText().isBlank()) { setStatus("Phone is required."); return false; }
        String email = emailField.getText().trim();
        if (email.isBlank())               { setStatus("Email is required."); return false; }
        if (!email.contains("@"))          { setStatus("Email is not valid."); return false; }
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
        Map<Long, PatientAddress> addressMap = addressDAO.findAll();
        for (Patient p : patients) rows.add(new PatientRow(p, addressMap.get(p.getId())));
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

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

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
