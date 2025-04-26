package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class ConfirmationDialogController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label messageLabel;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    @FXML
    private Button closeButton;

    private boolean result = false;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void initialize() {
        // Добавляем возможность перетаскивания окна 
        rootPane.setOnMousePressed((MouseEvent event) -> {
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
            rootPane.setCursor(Cursor.DEFAULT); 
        });

        rootPane.setOnMouseDragged((MouseEvent event) -> {
            Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });

        rootPane.setOnMouseReleased((MouseEvent _) -> {
            rootPane.setCursor(Cursor.DEFAULT); 
        });
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public boolean getResult() {
        return result;
    }

    @FXML
    private void handleYesButton() {
        result = true;
        closeDialog();
    }

    @FXML
    private void handleNoButton() {
        result = false;
        closeDialog();
    }

    @FXML
    private void handleCloseButton() {
        result = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
} 