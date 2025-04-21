package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Controller;

@Controller
public class ControlPanelController implements Initializable {

    @FXML
    private VBox controlPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        controlPanel.setVisible(false);
    }

}
