package controller;

import dao.PatientDAO;
import dao.PatientFeedbackDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Patient;
import model.PatientFeedback;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FeedbackController {

    @FXML private ComboBox<Patient> patientCombo;
    @FXML private TextArea          feedbackArea;
    @FXML private TextArea          notesArea;
    @FXML private TextField         searchField;
    @FXML private Label             statusLabel;

    @FXML private TableView<PatientFeedback>              feedbackTable;
    @FXML private TableColumn<PatientFeedback, String>    colId;
    @FXML private TableColumn<PatientFeedback, String>    colPatient;
    @FXML private TableColumn<PatientFeedback, String>    colFeedback;
    @FXML private TableColumn<PatientFeedback, String>    colNotes;
    @FXML private TableColumn<PatientFeedback, String>    colDate;

    private final PatientFeedbackDAO feedbackDAO = new PatientFeedbackDAO();
    private final PatientDAO         patientDAO  = new PatientDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<Patient> patients;

    @FXML
    public void initialize() {
        loadPatientCombo();
        setupColumns();
        loadAll();

        feedbackTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> { if (row != null) populateForm(row); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        try {
            PatientFeedback fb = new PatientFeedback(
                patientCombo.getValue().getId(),
                feedbackArea.getText().trim(),
                notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim(),
                LocalDateTime.now()
            );
            feedbackDAO.insert(fb);
            setStatus("Feedback submitted.");
            onClear();
            loadAll();
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    void onDelete() {
        PatientFeedback row = feedbackTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select a feedback entry to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete feedback #" + row.getId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    feedbackDAO.delete(row.getId());
                    onClear();
                    loadAll();
                    setStatus("Feedback deleted.");
                } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) { loadAll(); return; }
        try {
            List<PatientFeedback> results = feedbackDAO.findAll().stream()
                .filter(fb -> patientNameFor(fb.getPatientId()).toLowerCase()
                                .contains(keyword.toLowerCase()))
                .toList();
            feedbackTable.setItems(FXCollections.observableArrayList(results));
            setStatus("Found " + results.size() + " result(s).");
        } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML
    void onShowAll() { searchField.clear(); loadAll(); statusLabel.setText(""); }

    @FXML
    void onClear() {
        patientCombo.setValue(null);
        feedbackArea.clear();
        notesArea.clear();
        searchField.clear();
        statusLabel.setText("");
        feedbackTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        if (patientCombo.getValue() == null) { setStatus("Select a patient."); return false; }
        if (feedbackArea.getText().isBlank()) { setStatus("Feedback text is required."); return false; }
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

    private void setupColumns() {
        colId.setCellValueFactory(c      -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(patientNameFor(c.getValue().getPatientId())));
        colFeedback.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFeedback()));
        colNotes.setCellValueFactory(c   -> new SimpleStringProperty(nvl(c.getValue().getPrivateNotes())));
        colDate.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getIssuedAt().format(DATE_FMT)));
    }

    private void loadAll() {
        try {
            feedbackTable.setItems(FXCollections.observableArrayList(feedbackDAO.findAll()));
        } catch (SQLException e) { setStatus("Error loading feedback: " + e.getMessage()); }
    }

    private void populateForm(PatientFeedback fb) {
        if (patients != null) patients.stream()
            .filter(p -> p.getId() == fb.getPatientId())
            .findFirst().ifPresent(patientCombo::setValue);
        feedbackArea.setText(fb.getFeedback());
        notesArea.setText(nvl(fb.getPrivateNotes()));
        setStatus("Viewing feedback #" + fb.getId() + " — submit to create a new entry.");
    }

    private String patientNameFor(long id) {
        if (patients == null) return "";
        return patients.stream().filter(p -> p.getId() == id)
            .map(Patient::getName).findFirst().orElse("Unknown");
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
    private static String nvl(String s) { return s != null ? s : ""; }
}
