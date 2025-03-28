package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.MinioApplication;
import mente.nova.mente_nova.minio.MinioList.Node;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Контроллер для отображения содержимого предмета и навигации по файловой структуре.
 * Управляет отображением файлов, папок и навигационной панели.
 */
@Component
public class SubjectContentController implements Initializable {

    @FXML
    private Label subjectTitle;

    @FXML
    private Button backButton;
    
    @FXML
    private VBox contentContainer;

    @FXML
    private HBox pathPanel;
    
    @Autowired
    private MinioApplication minio;
    
    /**
     * Инициализация контроллера. Настраивает начальное состояние и загружает содержимое.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String path = ConfigManager.getValue("path");
        backButton.setOnAction(event -> goBack());
        createPathContainer(path);

        if (path.lastIndexOf("/") == path.length() - 1) {
            path = path.substring(0, path.length() - 1);
        }

        setSubjectName(path.substring(path.lastIndexOf("/") + 1, path.length()));
    }

    /**
     * Создает навигационную панель с путем к текущей директории.
     * Добавляет иконку дома и разделители между элементами пути.
     * @param path Текущий путь к директории
     */
    private void createPathContainer(String path) {
        String[] pathParts = path.split("/");
        HBox pathItem = new HBox();
        pathItem.getStyleClass().add("pathContainer");

        HBox homeContainer = new HBox();
        homeContainer.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/image/house_icon.png"))));
        homeContainer.getChildren().add(new Label("Главная"));
        homeContainer.getStyleClass().add("home-container");

        animationController.setupCardAnimationIncrease(homeContainer);
        homeContainer.setOnMouseClicked(event -> openSubjectCard("", true));

        pathItem.getChildren().add(homeContainer);
        pathItem.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/image/arrow_path.png"))));
        String currentPath = "";
        for (int i = 0; i < pathParts.length; i++) {
            currentPath += pathParts[i] + "/";
            Label pathLabel = new Label(pathParts[i]);
            if (i < pathParts.length - 1) {
                animationController.setupCardAnimationIncrease(pathLabel);
                final String finalPath = currentPath;
                pathLabel.setOnMouseClicked(event -> openSubjectCard(finalPath, true));
                pathItem.getChildren().add(pathLabel);
                pathItem.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/image/arrow_path.png"))));
            } else {
                pathLabel.getStyleClass().add("currentPath");
                pathItem.getChildren().add(pathLabel);
            }
        }
        pathPanel.getChildren().add(pathItem);
    }
    
    /**
     * Устанавливает название предмета и загружает соответствующее содержимое.
     * Определяет тип содержимого (директория или файл) и вызывает соответствующий метод загрузки.
     * @param subjectName Название предмета или файла
     */
    public void setSubjectName(String subjectName) {
        
        // Устанавливаем заголовок
        if (subjectTitle != null) {
            subjectTitle.setText(subjectName);
        }

        String path = ConfigManager.getValue("path");
        
        if (path.lastIndexOf("/")  == path.length()-1) {
            loadSubjectContent();
        } else {
            String fileName = path.substring(path.lastIndexOf("/") + 1, path.length());
            if (fileName.substring(fileName.lastIndexOf(".")).equals(".pdf")) {
                loadFileContent(fileName);
            } else {
                System.out.println("Это не pdf файл!");
            }
        }
    }
    
    /**
     * Загружает содержимое директории из MinIO.
     * Создает карточки для каждого элемента в директории.
     */
    private void loadSubjectContent() {
        try {
            // Загружаем файлы предмета из MinIO
            Node list = minio.list(ConfigManager.getValue("bucket"), ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path"), false);
            
            for (Node subjectName : list.getChildren().values()) {
                HBox subjectCard = createFileCard(subjectName.getName(), subjectName.isDirectory());
                contentContainer.getChildren().add(subjectCard);
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке содержимого предмета: " + e.getMessage());
        }
    }

    /**
     * Загружает содержимое файла.
     * В текущей реализации отображает только имя файла.
     * @param fileName Имя файла для отображения
     */
    private void loadFileContent(String fileName) {
        try {
           VBox fileContent = new VBox();
           fileContent.getStyleClass().add("fileContent");
           fileContent.getChildren().add(new Label("Имя файла: " + fileName));
           contentContainer.getChildren().add(fileContent);
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке содержимого файла: " + e.getMessage());
        }
    }

    /**
     * Создает карточку для отображения файла или директории.
     * @param fileName Имя файла или директории
     * @param isDirectory Флаг, указывающий является ли элемент директорией
     * @return HBox с созданной карточкой
     */
    private HBox createFileCard(String fileName, boolean isDirectory) {
        HBox subjectCard = new HBox();
        subjectCard.getStyleClass().add("cardStructure");
        
        VBox cardStructure = new VBox();
        cardStructure.setAlignment(Pos.CENTER);
        HBox.setHgrow(cardStructure, Priority.ALWAYS);
        cardStructure.setPadding(new Insets(0, 0, 0, 10));

        
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("imageContainer"); 
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/image/" +  (isDirectory ? "folder" : "file")+ "_icon.png")));
        imageContainer.getChildren().add(imageView);

        
        ImageView arrowImage = new ImageView(new Image(getClass().getResourceAsStream("/image/link_arrow.png")));
        HBox.setMargin(arrowImage, new Insets(0, 0, 0, 0));
        arrowImage.getStyleClass().add("arrow-icon");

        Label fileNameLabel = new Label(fileName);
        fileNameLabel.getStyleClass().add("subject");

        Label fileWeight = new Label("Последнее изменение: + -00:00 · 1000МБ");
        fileWeight.getStyleClass().add("description");

        cardStructure.getChildren().addAll(fileNameLabel, fileWeight);

        subjectCard.getChildren().addAll(imageContainer, cardStructure, arrowImage);

        subjectCard.setOnMouseClicked(event -> openSubjectCard(ConfigManager.getValue("path") + (ConfigManager.getValue("path").lastIndexOf("/") != -1 ? "" : "/") + fileName, isDirectory));

        return subjectCard;
    }

    /**
     * Обрабатывает открытие элемента (файла или директории).
     * Обновляет путь в ConfigManager и переключает отображение.
     * @param path Путь к открываемому элементу
     * @param isDirectory Флаг, указывающий является ли элемент директорией
     */
    private void openSubjectCard(String path, boolean isDirectory) {
        if (path.equals("")) {
            MainController.switchContent("section-content.fxml");
        } else {
            if (isDirectory) {
                if (path.lastIndexOf("/") != path.length() - 1) {
                    ConfigManager.setValue("path", path + "/");
                } else {
                    ConfigManager.setValue("path", path);
                }
                MainController.switchContent("subject-content.fxml");
            } else {
                ConfigManager.setValue("path", path);
                MainController.switchContent("subject-content.fxml");
            }
        }
        
    }

    /**
     * Обрабатывает нажатие кнопки "Назад".
     * Обновляет путь в ConfigManager и возвращается к предыдущему уровню.
     */
    @FXML
    private void goBack() {
        
        try {
            String path = ConfigManager.getValue("path");
            if (path.lastIndexOf("/") == path.length() - 1) {
                path = path.substring(0, path.length() - 1);
            }
            if (path.lastIndexOf("/") == -1) {
                ConfigManager.setValue("path", "");
            } else {
                ConfigManager.setValue("path", path.substring(0, path.lastIndexOf("/") + 1));
            }
            
            if (ConfigManager.getValue("path").equals("")) {
                MainController.switchContent("section-content.fxml");
            } else {
                MainController.switchContent("subject-content.fxml");
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка при открытии страницы предмета: " + e.getMessage());
            e.printStackTrace();
        }
    }

}