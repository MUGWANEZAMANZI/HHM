package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class HomeController {

    @FXML private StackPane contentArea;

    @FXML void showDashboard()    { /* welcome screen already showing */ contentArea.getChildren().clear(); }
    @FXML void showPatients()     { load("patient-view.fxml"); }
    @FXML void showDoctors()      { load("doctor-view.fxml"); }
    @FXML void showAppointments() { load("appointment-view.fxml"); }
    @FXML void showPrescriptions(){ load("prescription-view.fxml"); }
    @FXML void showInventory()    { load("inventory-view.fxml"); }

    private void load(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/hhm/" + fxml));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load " + fxml + ": " + e.getMessage());
        }
    }
}
