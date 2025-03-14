package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Controller;

@Controller
public class MainController implements Initializable {
    @FXML
    private TilePane subjectsTilePane;

    private String[] subjectNames = {
        "Математика", "Физика", "Химия", "Биология", 
        "История", "География", "Литература", "Информатика"
    };
    
    private int[] filesCount = {12, 8, 7, 5, 9, 6, 4, 10};
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateSubjectCards();
    }
    
    private void generateSubjectCards() {
        // Очищаем TilePane перед заполнением новыми элементами
        subjectsTilePane.getChildren().clear();
        
        // Проходим по массивам и создаем VBox для каждого предмета
        for (int i = 0; i < subjectNames.length; i++) {
            VBox subjectCard = createSubjectCard(subjectNames[i], filesCount[i]);
            subjectsTilePane.getChildren().add(subjectCard);
        }
    }
    
    private VBox createSubjectCard(String subjectName, int fileCount) {
        // Создаем VBox с нужным CSS классом
        VBox card = new VBox();
        card.getStyleClass().add("listSubjects");
        
        // Создаем ImageView с иконкой предмета
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/image/subjects_book.png")));
        
        // Создаем Label с названием предмета
        Label subjectLabel = new Label(subjectName);
        subjectLabel.getStyleClass().add("subject");
        
        // Создаем Label с количеством файлов
        Label descriptionLabel = new Label(fileCount + " файлов");
        descriptionLabel.getStyleClass().add("description");
        
        // Добавляем все элементы в карточку
        card.getChildren().addAll(imageView, subjectLabel, descriptionLabel);
        
        return card;
    } 
}