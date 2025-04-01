package mente.nova.mente_nova.controller;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import jakarta.annotation.PostConstruct;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер для отображения списка предметов и управления секциями.
 * Отвечает за загрузку и отображение карточек предметов.
 */
@Component
public class SectionController implements Initializable {

    private List<String> subjectNames = new ArrayList<>();
    private List<Integer> filesCount = new ArrayList<>();

    @Autowired
    private MinioApplication minio;

    /**
     * Инициализация списка предметов при создании контроллера.
     * Загружает информацию о предметах и количестве файлов из MinIO.
     */
    @PostConstruct
    private void initSubjects() {
        try {
            for (MinioList.Node subjectName : minio.list(ConfigManager.getValue("semester") + " семестр", false).getChildren().values()) {
                if (subjectName.isDirectory()) {
                    subjectNames.add(subjectName.getName());
                    filesCount.add(minio.list(ConfigManager.getValue("semester") + " семестр/" + subjectName.getName(), true).getChildren().values().size());
                }
            }
        } catch (Exception e) {
            Logger.error("Ошибка при получении списка предметов: " + e.getMessage());
        }
    }
    
    @FXML
    private TilePane subjectsTilePane;

    /**
     * Инициализация контроллера. Загружает начальное содержимое в зависимости от выбранного семестра.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ConfigManager.getValue("semester").equals("")) {
            generateStartPanel();
        } else {
            generateSubjectCards();
        }
    }

    /**
     * Генерирует начальную панель выбора семестра.
     */
    private void generateStartPanel() {
        subjectsTilePane.getChildren().clear();

        
    }
    
    /**
     * Генерирует карточки предметов для отображения.
     */
    private void generateSubjectCards() {
        // Очищаем TilePane перед заполнением новыми элементами
        subjectsTilePane.getChildren().clear();
        
        // Проходим по массивам и создаем VBox для каждого предмета
        for (int i = 0; i < subjectNames.size(); i++) {
            VBox subjectCard = createSubjectCard(subjectNames.get(i), filesCount.get(i));
            subjectsTilePane.getChildren().add(subjectCard);
        }
    }

    /**
     * Создает карточку для отображения предмета.
     * @param subjectName Название предмета
     * @param fileCount Количество файлов в предмете
     * @return VBox с созданной карточкой
     */
    private VBox createSubjectCard(String subjectName, int fileCount) {
        
        // Создаем VBox с нужным CSS классом
        VBox card = new VBox();
        card.getStyleClass().add("listSubjects");

        animationController.setupCardAnimationLifting(card);
        
        // Создаем ImageView с иконкой предмета
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/image/subjects_book.png")));
        
        if (subjectName.length() > 27) {
            for (int i = 27; i > 0; i--) {
                if (subjectName.charAt(i) == ' ') {
                    subjectName = subjectName.substring(0, i) + "\n" + subjectName.substring(i + 1);
                    break;
                }
            }
        }
        // Создаем Label с названием предмета
        Label subjectLabel = new Label(subjectName);
        subjectLabel.getStyleClass().add("subject");
        
        String spellingFiles;
        if (fileCount == 0) {
            spellingFiles = "файлов";
        } else if (fileCount == 1) {
            spellingFiles = "файл";
        } else if (fileCount > 1 && fileCount < 5) {
            spellingFiles = "файла";
        } else {
            spellingFiles = "файлов";
        }
        // Создаем Label с количеством файлов
        Label descriptionLabel = new Label(fileCount + " " + spellingFiles);
        descriptionLabel.getStyleClass().add("description");
        
        // Добавляем все элементы в карточку
        card.getChildren().addAll(imageView, subjectLabel, descriptionLabel);
        // Добавляем обработчик нажатия на карточку
        final String finalSubjectName = subjectName.replace("\n", " ");
        card.setOnMouseClicked(_ -> openSubjectContent(finalSubjectName));
        
        return card;
    }

    /**
     * Открывает страницу с содержимым предмета.
     * @param subjectName Название предмета
     */
    private void openSubjectContent(String subjectName) {
        try {
            ConfigManager.setValue("path", subjectName + "/");
            
            // Вызываем статический метод через getInstance
            MainController.switchContent("subject-content.fxml");
            
        } catch (Exception e) {
            Logger.error("Ошибка при открытии страницы предмета: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
