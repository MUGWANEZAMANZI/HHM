package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import java.io.IOException;

public class HomeController {

    @FXML private StackPane contentArea;

// Label for displaying the current time
    @FXML private Label time;


    // Node for tracking Dashboard
    private Node dashboard;

    @FXML void showDashboard()    { /* welcome screen already showing */ contentArea.getChildren().setAll(dashboard); }
    @FXML void showPatients()     { load("patient-view.fxml"); }
    @FXML void showDoctors()      { load("doctor-view.fxml"); }
    @FXML void showAppointments() { load("appointment-view.fxml"); }
    @FXML void showPrescriptions(){ load("prescription-view.fxml"); }
    @FXML void showInventory()    { load("inventory-view.fxml"); }
    @FXML void showFeedback()     { load("feedback-view.fxml"); }

    private void load(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/hhm/" + fxml));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load " + fxml + ": " + e.getMessage());
        }
    }

    @FXML
    public void initialize()
    {
        dashboard = contentArea.getChildren().get(0);  // Store the initial dashboard node

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                time.setText(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        };
        timer.start();
    }
}
