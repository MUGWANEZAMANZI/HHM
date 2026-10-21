package controller;

import dao.DoctorDAO;
import dao.PatientDAO;
import dao.PrescriptionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Doctor;
import model.Patient;
import model.Prescription;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrescriptionController {

    @FXML private ComboBox<Patient> patientCombo;
    @FXML private ComboBox<Doctor>  doctorCombo;
    @FXML private DatePicker        datePicker;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private ComboBox<String>  filterStatus;
    @FXML private TextArea          notesArea;
    @FXML private TextField         searchField;
    @FXML private Button            saveBtn;
    @FXML private Label             statusLabel;

    @FXML private TableView<Prescription>              prescriptionTable;
    @FXML private TableColumn<Prescription, String>    colId;
    @FXML private TableColumn<Prescription, String>    colPatient;
    @FXML private TableColumn<Prescription, String>    colDoctor;
    @FXML private TableColumn<Prescription, String>    colDate;
    @FXML private TableColumn<Prescription, String>    colStatus;
    @FXML private TableColumn<Prescription, String>    colNotes;

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final PatientDAO      patientDAO      = new PatientDAO();
    private final DoctorDAO       doctorDAO       = new DoctorDAO();

    private static final List<String> STATUSES = List.of("Active", "Completed", "Cancelled");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<Patient> patients;
    private List<Doctor>  doctors;
    private long selectedId = -1;

    @FXML
    public void initialize() {
        loadPatientCombo();
        loadDoctorCombo();
        statusCombo.setItems(FXCollections.observableArrayList(STATUSES));
        statusCombo.setValue("Active");
        filterStatus.setItems(FXCollections.observableArrayList(
            List.of("All", "Active", "Completed", "Cancelled")));
        filterStatus.setValue("All");
        setupColumns();
        loadAll();

        prescriptionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> { if (row != null) populateForm(row); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        try {
            LocalDateTime issuedAt = LocalDateTime.of(datePicker.getValue(), LocalTime.NOON);
            Prescription p = new Prescription(
                patientCombo.getValue().getId(),
                doctorCombo.getValue().getId(),
                issuedAt,
                statusCombo.getValue(),
                notesArea.getText().trim()
            );
            if (selectedId == -1) {
                prescriptionDAO.insert(p);
                setStatus("Prescription issued.");
            } else {
                p.setId(selectedId);
                prescriptionDAO.update(p);
                setStatus("Prescription updated.");
            }
            onClear();
            loadAll();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    void onDelete() {
        Prescription row = prescriptionTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select a prescription to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete prescription #" + row.getId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    prescriptionDAO.delete(row.getId());
                    onClear();
                    loadAll();
                    setStatus("Prescription deleted.");
                } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        String filter  = filterStatus.getValue();
        try {
            List<Prescription> results;
            if (!keyword.isBlank()) {
                // search by patient or doctor id via name lookup
                results = prescriptionDAO.findAll().stream()
                    .filter(p -> patientNameFor(p.getPatientId()).toLowerCase()
                                   .contains(keyword.toLowerCase())
                              || doctorNameFor(p.getDoctorId()).toLowerCase()
                                   .contains(keyword.toLowerCase()))
                    .toList();
            } else if (filter != null && !filter.equals("All")) {
                results = prescriptionDAO.findAll().stream()
                    .filter(p -> filter.equals(p.getStatus())).toList();
            } else {
                loadAll(); return;
            }
            prescriptionTable.setItems(FXCollections.observableArrayList(results));
        } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML void onShowAll() { searchField.clear(); filterStatus.setValue("All"); loadAll(); }

    @FXML
    void onClear() {
        patientCombo.setValue(null);
        doctorCombo.setValue(null);
        datePicker.setValue(null);
        statusCombo.setValue("Active");
        notesArea.clear();
        selectedId = -1;
        saveBtn.setText("Issue Prescription");
        statusLabel.setText("");
        prescriptionTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        if (patientCombo.getValue() == null) { setStatus("Select a patient."); return false; }
        if (doctorCombo.getValue()  == null) { setStatus("Select a doctor.");  return false; }
        if (datePicker.getValue()   == null) { setStatus("Select a date.");    return false; }
        return true;
    }

    private void loadPatientCombo() {
        try {
            patients = patientDAO.findAll();
            patientCombo.setItems(FXCollections.observableArrayList(patients));
            patientCombo.setCellFactory(lv -> new ListCell<>() {
                protected void updateItem(Patient p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty || p == null ? "" : p.getName());
                }
            });
            patientCombo.setButtonCell(patientCombo.getCellFactory().call(null));
        } catch (SQLException e) { setStatus("Could not load patients."); }
    }

    private void loadDoctorCombo() {
        try {
            doctors = doctorDAO.findAll();
            doctorCombo.setItems(FXCollections.observableArrayList(doctors));
            doctorCombo.setCellFactory(lv -> new ListCell<>() {
                protected void updateItem(Doctor d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? "" : d.getName());
                }
            });
            doctorCombo.setButtonCell(doctorCombo.getCellFactory().call(null));
        } catch (SQLException e) { setStatus("Could not load doctors."); }
    }

    private void setupColumns() {
        colId.setCellValueFactory(c      -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(patientNameFor(c.getValue().getPatientId())));
        colDoctor.setCellValueFactory(c  -> new SimpleStringProperty(doctorNameFor(c.getValue().getDoctorId())));
        colDate.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getIssuedAt().format(DATE_FMT)));
        colNotes.setCellValueFactory(c   -> new SimpleStringProperty(nvl(c.getValue().getNotes())));
        colStatus.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "Cancelled"  -> "-fx-text-fill:#e53935; -fx-font-weight:bold;";
                    case "Completed"  -> "-fx-text-fill:#2e7d32; -fx-font-weight:bold;";
                    default           -> "-fx-text-fill:#1976D2; -fx-font-weight:bold;";
                });
            }
        });
    }

    private void loadAll() {
        try {
            prescriptionTable.setItems(FXCollections.observableArrayList(prescriptionDAO.findAll()));
        } catch (SQLException e) { setStatus("Error loading prescriptions: " + e.getMessage()); }
    }

    private void populateForm(Prescription p) {
        selectedId = p.getId();
        if (patients != null) patients.stream().filter(pt -> pt.getId() == p.getPatientId())
            .findFirst().ifPresent(patientCombo::setValue);
        if (doctors  != null) doctors.stream().filter(d  -> d.getId()  == p.getDoctorId())
            .findFirst().ifPresent(doctorCombo::setValue);
        datePicker.setValue(p.getIssuedAt().toLocalDate());
        statusCombo.setValue(p.getStatus());
        notesArea.setText(nvl(p.getNotes()));
        saveBtn.setText("Update Prescription");
        setStatus("Editing prescription #" + p.getId());
    }

    private String patientNameFor(long id) {
        if (patients == null) return "";
        return patients.stream().filter(p -> p.getId() == id)
            .map(Patient::getName).findFirst().orElse("Unknown");
    }

    private String doctorNameFor(long id) {
        if (doctors == null) return "";
        return doctors.stream().filter(d -> d.getId() == id)
            .map(Doctor::getName).findFirst().orElse("Unknown");
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
    private static String nvl(String s) { return s != null ? s : ""; }
}
