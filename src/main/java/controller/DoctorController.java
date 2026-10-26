package controller;

import dao.DepartmentDAO;
import dao.DoctorDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Department;
import model.Doctor;
import service.DoctorService;
import service.ServiceException;

import java.sql.SQLException;
import java.util.List;

public class DoctorController {

    @FXML private TextField nameField;
    @FXML private TextField licenseField;
    @FXML private ComboBox<Department> departmentCombo;
    @FXML private TextField searchField;
    @FXML private Button saveBtn;
    @FXML private Label statusLabel;

    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, String> colId;
    @FXML private TableColumn<Doctor, String> colName;
    @FXML private TableColumn<Doctor, String> colDepartment;
    @FXML private TableColumn<Doctor, String> colLicense;

    private final DoctorService  doctorService  = new DoctorService();
    private final DoctorDAO      doctorDAO      = new DoctorDAO();
    private final DepartmentDAO  departmentDAO  = new DepartmentDAO();

    private long selectedDoctorId = -1;
    private List<Department> departments;

    @FXML
    public void initialize() {
        loadDepartments();
        setupColumns();
        loadAll();
        doctorTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, doc) -> { if (doc != null) populateForm(doc); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        try {
            Doctor d = new Doctor(
                nameField.getText().trim(),
                departmentCombo.getValue().getId(),
                licenseField.getText().trim()
            );
            if (selectedDoctorId == -1) {
                doctorService.register(d);
                setStatus("Doctor saved successfully.");
            } else {
                d.setId(selectedDoctorId);
                doctorService.update(d);
                setStatus("Doctor updated successfully.");
            }
            onClear();
            loadAll();
        } catch (ServiceException e) {
            setStatus(e.getMessage());
        }
    }

    @FXML
    void onDelete() {
        Doctor selected = doctorTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("Select a doctor to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete doctor \"" + selected.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    doctorService.remove(selected.getId());
                    onClear();
                    loadAll();
                    setStatus("Doctor deleted.");
                } catch (ServiceException e) { setStatus(e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) { loadAll(); return; }
        try {
            doctorTable.setItems(FXCollections.observableArrayList(doctorService.search(keyword)));
        } catch (ServiceException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML void onShowAll() { searchField.clear(); loadAll(); }

    @FXML
    void onClear() {
        nameField.clear(); licenseField.clear();
        departmentCombo.setValue(null);
        selectedDoctorId = -1;
        saveBtn.setText("Save Doctor");
        statusLabel.setText("");
        doctorTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        if (nameField.getText().isBlank())    { setStatus("Name is required."); return false; }
        if (licenseField.getText().isBlank()) { setStatus("License number is required."); return false; }
        if (departmentCombo.getValue() == null) { setStatus("Please select a department."); return false; }
        return true;
    }

    private void loadDepartments() {
        try {
            departments = departmentDAO.findAll();
            departmentCombo.setItems(FXCollections.observableArrayList(departments));
            departmentCombo.setCellFactory(lv -> new ListCell<>() {
                protected void updateItem(Department d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? "" : d.getName());
                }
            });
            departmentCombo.setButtonCell(new ListCell<>() {
                protected void updateItem(Department d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? "" : d.getName());
                }
            });
        } catch (SQLException e) { setStatus("Could not load departments: " + e.getMessage()); }
    }

    private void setupColumns() {
        colId.setCellValueFactory(c         -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getName()));
        colLicense.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getLicenseNumber()));
        colDepartment.setCellValueFactory(c -> new SimpleStringProperty(departmentNameFor(c.getValue().getDepartmentId())));
    }

    private void loadAll() {
        try {
            doctorTable.setItems(FXCollections.observableArrayList(doctorDAO.findAll()));
        } catch (SQLException e) { setStatus("Error loading doctors: " + e.getMessage()); }
    }

    private void populateForm(Doctor doc) {
        selectedDoctorId = doc.getId();
        nameField.setText(doc.getName());
        licenseField.setText(doc.getLicenseNumber());
        if (departments != null) departments.stream()
            .filter(d -> d.getId() == doc.getDepartmentId())
            .findFirst().ifPresent(departmentCombo::setValue);
        saveBtn.setText("Update Doctor");
        setStatus("Editing doctor #" + doc.getId());
    }

    private String departmentNameFor(long id) {
        if (departments == null) return "";
        return departments.stream().filter(d -> d.getId() == id)
            .map(Department::getName).findFirst().orElse("Unknown");
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
}
