package mente.nova.mente_nova.controller;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.*;
import mente.nova.mente_nova.view.BasicTools;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import mente.nova.mente_nova.controller.MainController.DataReceiver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import jakarta.annotation.PostConstruct;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Контроллер для отображения списка предметов и управления секциями.
 * Отвечает за загрузку и отображение карточек предметов.
 */
@Component
public class SectionController implements Initializable, DataReceiver {

    private List<String> subjectNames = new ArrayList<>();
    private List<Integer> filesCount = new ArrayList<>();

    @FXML
    private TilePane subjectsTilePane;

    @FXML
    private Button addSubjectButton;

    @FXML
    private TextField searchTextField;

    @FXML
    private VBox mainPanel;

    @FXML
    private StackPane sectionMenu;

    @Autowired
    private MinioApplication minio;

    /**
     * Инициализация списка предметов при создании контроллера.
     * Загружает информацию о предметах и количестве файлов из MinIO.
     */
    @PostConstruct
    private void initSubjects() {
        try {
            subjectNames.clear();
            filesCount.clear();
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

    /**
     * Инициализация контроллера. Загружает начальное содержимое в зависимости от выбранного семестра.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateTextField();
        
        if (ConfigManager.getValue("semester").equals("")) {
            generateStartPanel();
        } else {
            generateSubjectCards();
        }
    }

    public void generateTextField() {
        List<Character> alphabet = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z','А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л',
            'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь',
            'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л',
            'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь',
            'э', 'ю', 'я', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '_', '-', ' ');
        searchTextField.setPromptText("Добавить предмет...");
        searchTextField.setStyle("-fx-prompt-text-fill: #b2b2b2;");
        searchTextField.getStyleClass().add("searchTextField");
        
        // Настраиваем анимацию поля поиска
        animationController.setupSearchFieldAnimation(searchTextField);
        
        // Обработчик нажатия на кнопку - показать/скрыть поле
        addSubjectButton.setOnAction(_ -> {
            if (searchTextField.isVisible() && searchTextField.getOpacity() > 0) {
                if (searchTextField.getText().isEmpty()) {
                    searchTextField.setPromptText("Введите название предмета");
                    searchTextField.setStyle("-fx-prompt-text-fill:#f6533c;");
                } else {
                    if (BasicTools.containsOnlyAllowedChars(searchTextField.getText(), alphabet)) {
                        createSubject();
                        animationController.hideSearchField(searchTextField);
                    } else {
                        MainController.showNotification("warning", "Название предмета содержит недопустимые символы");
                    }
                }
            } else {
                animationController.showSearchField(searchTextField);
                searchTextField.setPromptText("Добавить предмет...");
                searchTextField.setStyle("-fx-prompt-text-fill: #b2b2b2;");
            }
        });

        searchTextField.setOnAction(_ -> {
            if (searchTextField.getText().isEmpty()) {
                searchTextField.setPromptText("Введите название предмета");
                searchTextField.setStyle("-fx-prompt-text-fill:#f6533c;");
            } else {
                if (BasicTools.containsOnlyAllowedChars(searchTextField.getText(), alphabet)) {
                    //minio.annihilateFolder(searchTextField.getText());
                    createSubject();
                    animationController.hideSearchField(searchTextField);
                } else {
                    MainController.showNotification("warning", "Название предмета содержит недопустимые символы");
                }
            }
        });
        
        // Скрыть поле при клике на пустое пространство
        subjectsTilePane.setOnMousePressed(_ -> {
            if (searchTextField.isVisible() && searchTextField.getOpacity() > 0) {
                animationController.hideSearchField(searchTextField);
            }
        });
    }

    public void createSubject() {
        try {
            // Создаем пустую папку в minio
            minio.createEmptyFolder(ConfigManager.getValue("semester") + " семестр/" + searchTextField.getText());
            // Создаем карточку предмета
            VBox subjectCard = createSubjectCard(searchTextField.getText(), 0);
            // Запускаем анимацию появления карточки
            animationController.animateNewElementAppearance(subjectCard);
            // Добавляем карточку в контейнер
            subjectsTilePane.getChildren().add(subjectCard);
            //Выводим уведомление о создании предмета
            MainController.showNotification("success", "Предмет \"" + searchTextField.getText() + "\" создан");
            // Очищаем поле ввода
            searchTextField.setText("");
        } catch (Exception e) {
            Logger.error("Ошибка при создании предмета: " + e.getMessage());
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
            
            // Вызываем статический метод с передачей данных предмета
            MainController.switchContent("subject-content.fxml", subjectName);
            
        } catch (Exception e) {
            Logger.error("Ошибка при открытии страницы предмета: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Получает данные от MainController при переключении сцены.
     * @param data Данные, переданные от MainController
     */
    @Override
    public void receiveData(Object data) {
        Logger.info("Получены данные в SectionController: " + data);
        
        // Обрабатываем полученные данные в зависимости от их типа
        if (data instanceof String) {
            String dataString = (String) data;
            if (dataString.substring(0, dataString.indexOf('_')).equals("deleteFolder")) {
                // Удаляем предмет
                dataString = dataString.substring(dataString.lastIndexOf('_') + 1);
                initSubjects();
                generateSubjectCards();
            } else if (dataString.substring(0, dataString.indexOf('_')).equals("updateSubjects")) {
                // Обновляем список предметов
                initSubjects();
                generateSubjectCards();
            } else {
                Logger.error("Полученная команда неизвестна: " + dataString);
            }
        } else if (data instanceof List) {
            // Обработка для списка данных
            Logger.info("Получен список данных");
            // Реализация обработки списка
        } else if (data != null) {
            Logger.info("Получены данные неизвестного типа: " + data.getClass().getName());
        }
    }

}