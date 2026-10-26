package controller;

import dao.DoctorDAO;
import dao.PatientDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Appointment;
import model.AppointmentView;
import model.Doctor;
import model.Patient;
import service.AppointmentService;
import service.ServiceException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AppointmentController {

    @FXML private ComboBox<Patient>  patientCombo;
    @FXML private ComboBox<Doctor>   doctorCombo;
    @FXML private DatePicker         datePicker;
    @FXML private TextField          timeField;
    @FXML private ComboBox<String>   statusCombo;
    @FXML private ComboBox<String>   filterStatus;
    @FXML private TextField          searchField;
    @FXML private Button             saveBtn;
    @FXML private Label              statusLabel;

    @FXML private TableView<AppointmentView>              appointmentTable;
    @FXML private TableColumn<AppointmentView, String>    colId;
    @FXML private TableColumn<AppointmentView, String>    colPatient;
    @FXML private TableColumn<AppointmentView, String>    colPhone;
    @FXML private TableColumn<AppointmentView, String>    colDoctor;
    @FXML private TableColumn<AppointmentView, String>    colDepartment;
    @FXML private TableColumn<AppointmentView, String>    colDate;
    @FXML private TableColumn<AppointmentView, String>    colTime;
    @FXML private TableColumn<AppointmentView, String>    colStatus;

    private final AppointmentService appointmentService = new AppointmentService();
    private final PatientDAO         patientDAO         = new PatientDAO();
    private final DoctorDAO          doctorDAO          = new DoctorDAO();

    private static final List<String> STATUSES =
        List.of("Scheduled", "Completed", "Cancelled", "Rescheduled");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private long selectedId = -1;

    @FXML
    public void initialize() {
        loadPatientCombo();
        loadDoctorCombo();
        statusCombo.setItems(FXCollections.observableArrayList(STATUSES));
        statusCombo.setValue("Scheduled");
        filterStatus.setItems(FXCollections.observableArrayList(
            List.of("All", "Scheduled", "Completed", "Cancelled", "Rescheduled")));
        filterStatus.setValue("All");
        setupColumns();
        loadAll();
        appointmentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> { if (row != null) populateForm(row); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        LocalDateTime dt = buildDateTime();
        if (dt == null) return;
        try {
            Appointment a = new Appointment(
                patientCombo.getValue().getId(),
                doctorCombo.getValue().getId(),
                dt, statusCombo.getValue());
            if (selectedId == -1) {
                appointmentService.book(a);
                setStatus("Appointment booked successfully.");
            } else {
                a.setId(selectedId);
                appointmentService.reschedule(a);
                setStatus("Appointment rescheduled.");
            }
            onClear(); loadAll();
        } catch (ServiceException e) { setStatus(e.getMessage()); }
    }

    @FXML
    void onCancel() {
        AppointmentView row = appointmentTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select an appointment to cancel."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Cancel appointment for \"" + row.getPatientName() + "\"?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    appointmentService.cancel(row.getId());
                    loadAll();
                    setStatus("Appointment cancelled.");
                } catch (ServiceException e) { setStatus(e.getMessage()); }
            }
        });
    }

    @FXML
    void onDelete() {
        AppointmentView row = appointmentTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select an appointment to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Permanently delete appointment #" + row.getId() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    appointmentService.remove(row.getId());
                    onClear(); loadAll();
                    setStatus("Appointment deleted.");
                } catch (ServiceException e) { setStatus(e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        String filter  = filterStatus.getValue();
        try {
            List<AppointmentView> results;
            if (!keyword.isBlank())
                results = appointmentService.search(keyword);
            else if (filter != null && !filter.equals("All"))
                results = appointmentService.findByStatus(filter);
            else { loadAll(); return; }
            appointmentTable.setItems(FXCollections.observableArrayList(results));
        } catch (ServiceException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML void onShowAll() { searchField.clear(); filterStatus.setValue("All"); loadAll(); }

    @FXML
    void onClear() {
        patientCombo.setValue(null); doctorCombo.setValue(null);
        datePicker.setValue(null); timeField.clear();
        statusCombo.setValue("Scheduled");
        selectedId = -1;
        saveBtn.setText("Book Appointment");
        statusLabel.setText("");
        appointmentTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        if (patientCombo.getValue() == null) { setStatus("Select a patient."); return false; }
        if (doctorCombo.getValue()  == null) { setStatus("Select a doctor.");  return false; }
        if (datePicker.getValue()   == null) { setStatus("Select a date.");    return false; }
        if (timeField.getText().isBlank())   { setStatus("Enter time (HH:mm)."); return false; }
        return true;
    }

    private LocalDateTime buildDateTime() {
        try {
            LocalDate date = datePicker.getValue();
            LocalTime time = LocalTime.parse(timeField.getText().trim(), TIME_FMT);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            setStatus("Invalid time format — use HH:mm (e.g. 09:30)."); return null;
        }
    }

    private void loadPatientCombo() {
        try {
            List<Patient> patients = patientDAO.findAll();
            patientCombo.setItems(FXCollections.observableArrayList(patients));
            patientCombo.setCellFactory(lv -> new ListCell<>() {
                protected void updateItem(Patient p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty || p == null ? "" : p.getName() + " (" + p.getPhone() + ")");
                }
            });
            patientCombo.setButtonCell(patientCombo.getCellFactory().call(null));
        } catch (SQLException e) { setStatus("Could not load patients."); }
    }

    private void loadDoctorCombo() {
        try {
            List<Doctor> doctors = doctorDAO.findAll();
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
        colId.setCellValueFactory(c         -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getPatientName()));
        colPhone.setCellValueFactory(c      -> new SimpleStringProperty(nvl(c.getValue().getPatientPhone())));
        colDoctor.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getDoctorName()));
        colDepartment.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDepartmentName())));
        colDate.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getAppointmentDate().format(DATE_FMT)));
        colTime.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getAppointmentDate().format(TIME_FMT)));
        colStatus.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "Cancelled"  -> "-fx-text-fill:#e53935; -fx-font-weight:bold;";
                    case "Completed"   -> "-fx-text-fill:#2e7d32; -fx-font-weight:bold;";
                    case "Rescheduled" -> "-fx-text-fill:#f57c00; -fx-font-weight:bold;";
                    default            -> "-fx-text-fill:#1976D2; -fx-font-weight:bold;";
                });
            }
        });
    }

    private void loadAll() {
        try {
            appointmentTable.setItems(FXCollections.observableArrayList(appointmentService.getAll()));
        } catch (ServiceException e) { setStatus("Error loading appointments: " + e.getMessage()); }
    }

    private void populateForm(AppointmentView row) {
        selectedId = row.getId();
        patientCombo.getItems().stream().filter(p -> p.getId() == row.getPatientId())
            .findFirst().ifPresent(patientCombo::setValue);
        doctorCombo.getItems().stream().filter(d -> d.getId() == row.getDoctorId())
            .findFirst().ifPresent(doctorCombo::setValue);
        datePicker.setValue(row.getAppointmentDate().toLocalDate());
        timeField.setText(row.getAppointmentDate().format(TIME_FMT));
        statusCombo.setValue(row.getStatus());
        saveBtn.setText("Reschedule");
        setStatus("Editing appointment #" + row.getId());
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
    private static String nvl(String s) { return s != null ? s : ""; }
}
