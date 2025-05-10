package mente.nova.mente_nova.controller;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.service.*;
import mente.nova.mente_nova.view.BasicTools;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
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
import java.io.File;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

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

    @FXML
    private StackPane sectionContent;

    @FXML
    private StackPane centerContentContainer;

    @FXML
    private HBox semesterBox;

    @FXML
    private Button changeSemester;

    @Autowired
    private MinioApplication minio;

    // Добавляем свойства для выбора семестра
    private String selectedSemester;

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
        if (ConfigManager.getValue("semester").equals("")) {
            generateStartPanel();
        } else {
            // Семестр уже выбран, настраиваем основной вид
            setupMainView();
            initSubjects(); // Загружаем предметы
            generateSubjectCards(false); // Отображаем карточки
            dragAndDrop(); // Включаем Drag and Drop
        }
    }

    /** 
     * Настраивает основной вид (когда семестр выбран). 
     * Делает видимыми нужные элементы и настраивает обработчики.
     */
    private void setupMainView() {
        addSubjectButton.setVisible(true);
        addSubjectButton.setManaged(true);

        changeSemester.setVisible(true);
        changeSemester.setManaged(true);
        changeSemester.setOnAction(event -> {
            ConfigManager.setValue("semester", "");
            generateStartPanel();
        });

        searchTextField.setVisible(false); // Поле поиска изначально скрыто
        searchTextField.setManaged(false);

        setupSearchInteractions(); // Настройка логики показа/скрытия поиска
    }

    /**
     * Настраивает обработчики для кнопки добавления и поля поиска.
     */
    private void setupSearchInteractions() {
        List<Character> alphabet = getAlphabet(); // Вынес получение алфавита в отдельный метод
        
        // Обработчик нажатия на кнопку "Добавить предмет"
        addSubjectButton.setOnAction(_ -> {
            boolean isSearchVisible = searchTextField.isVisible() && searchTextField.getOpacity() > 0;
            if (isSearchVisible) {
                handleSubjectCreation(alphabet);
            } else {
                animationController.animateSearchToggle(changeSemester, searchTextField, true); // Показать поле
                searchTextField.setPromptText("Добавить предмет...");
                searchTextField.setStyle("-fx-prompt-text-fill: #b2b2b2;");
            }
        });

        // Обработчик нажатия Enter в поле поиска
        searchTextField.setOnAction(_ -> {
            handleSubjectCreation(alphabet);
        });
        
        // Скрыть поле при клике на пустое пространство (на TilePane)
        subjectsTilePane.setOnMousePressed(_ -> {
            if (searchTextField.isVisible() && searchTextField.getOpacity() > 0) {
                animationController.animateSearchToggle(changeSemester, searchTextField, false); // Скрыть поле
            }
        });
    }

    /**
     * Обрабатывает логику создания предмета после ввода в поле поиска.
     * @param alphabet Список допустимых символов.
     */
    private void handleSubjectCreation(List<Character> alphabet) {
        if (searchTextField.getText().isEmpty()) {
            searchTextField.setPromptText("Введите название предмета");
            searchTextField.setStyle("-fx-prompt-text-fill:#f6533c;");
        } else {
            String subjectName = searchTextField.getText();
            if (!BasicTools.containsOnlyAllowedChars(subjectName, alphabet)) {
                MainController.showNotification("warning", "Название предмета содержит недопустимые символы");
            } else if (minio.listChildren(ConfigManager.getValue("semester") + " семестр/", false).contains(subjectName)) {
                MainController.showNotification("warning", "Предмет с заданным названием уже существует");
            } else if (subjectName.charAt(0) == ' ' || subjectName.charAt(0) == '_' || subjectName.charAt(0) == '-') {
                MainController.showNotification("warning", "Название предмета не должно начинаться со специальных символов");
            } else if (subjectName.length() >= 79) {
                MainController.showNotification("warning", "Название предмета слишком длинное");
            } else {
                createSubject(); // Создаем предмет
                animationController.animateSearchToggle(changeSemester, searchTextField, false); // Скрываем поле
            }
        }
    }

    /**
     * Возвращает список допустимых символов для названия предмета.
     */
    private List<Character> getAlphabet() {
        return Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
            'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z','А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К', 'Л',
            'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь',
            'Э', 'Ю', 'Я', 'а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з', 'и', 'й', 'к', 'л',
            'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь',
            'э', 'ю', 'я', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', '_', '-', ' ');
    }

    /**
     * Инициализирует и настраивает функциональность Drag and Drop для загрузки файлов и папок.
     * Файлы загружаются в директорию текущего семестра в формате: "{номер_семестра} семестр/{имя_файла}"
     */
    private void dragAndDrop() {
        StackPane dropOverlay = new StackPane();
        dropOverlay.setStyle("-fx-background-color: rgba(34, 197, 94, 0.35); -fx-background-radius: 10;");
        dropOverlay.setVisible(false);
        dropOverlay.setMouseTransparent(true);

        sectionContent.getChildren().add(dropOverlay);

        sectionContent.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            boolean isSingleFolder = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (files.size() == 1 && files.get(0).isDirectory()) {
                    isSingleFolder = true;
                }
            }

            if (isSingleFolder) {
                event.acceptTransferModes(TransferMode.ANY);
                dropOverlay.setVisible(true);
            } else {
                dropOverlay.setVisible(false);
            }
            event.consume();
        });

        sectionContent.setOnDragExited(_ -> {
            dropOverlay.setVisible(false);
        });

        sectionContent.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                if (files.size() == 1 && files.get(0).isDirectory()) {
                    try {
                        File folder = files.get(0);
                        createSubject(folder.getName(), folder.getAbsolutePath());
                        success = true;
                    } catch (Exception e) {
                        Logger.error("Ошибка при загрузке папки: " + e.getMessage());
                    }
                }
            }
            
            // Скрываем подложку после сброса
            dropOverlay.setVisible(false);
            
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * Создает новую карточку предмета и добавляет её в интерфейс.
     */
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
            Logger.info("Предмет \"" + searchTextField.getText() + "\" создан");
            // Очищаем поле ввода
            searchTextField.setText("");
        } catch (Exception e) {
            Logger.error("Ошибка при создании предмета: " + e.getMessage());
        }
    }

    public void createSubject(String subjectName, String localPath) {
        try {
            // Создаем пустую папку в minio
            minio.createEmptyFolder(ConfigManager.getValue("semester") + " семестр/" + subjectName);
            // Загружаем папку в minio
            minio.loadingFolder(ConfigManager.getValue("semester") + " семестр/" + subjectName + "/", localPath);
            // Создаем карточку предмета
            VBox subjectCard = createSubjectCard(subjectName, minio.countFiles(ConfigManager.getValue("semester") + " семестр/" + subjectName + "/"));
            // Запускаем анимацию появления карточки
            animationController.animateNewElementAppearance(subjectCard);
            // Добавляем карточку в контейнер
            subjectsTilePane.getChildren().add(subjectCard);
            //Выводим уведомление о создании предмета
            Logger.info("Предмет \"" + subjectName + "\" создан");
        } catch (Exception e) {
            Logger.error("Ошибка при создании предмета: " + e.getMessage());
        }
    }

    /**
     * Генерирует начальную панель выбора семестра.
     */
    private void generateStartPanel() {
        // Скрываем элементы основного вида
        addSubjectButton.setVisible(false);
        addSubjectButton.setManaged(false);
        changeSemester.setVisible(false);
        changeSemester.setManaged(false);
        searchTextField.setVisible(false);
        searchTextField.setManaged(false);
        
        subjectsTilePane.getChildren().clear();
        
        // Создание панели выбора семестра (логика остается прежней)
        VBox semesterSelectionPanel = new VBox(); // Используем локальную переменную
        semesterSelectionPanel.setAlignment(Pos.CENTER);
        semesterSelectionPanel.getStyleClass().add("semester-panel");
        
        VBox titleBox = new VBox(10);
        titleBox.setAlignment(Pos.CENTER);
        Label questionLabel = new Label("Какой семестр хотите выбрать?");
        questionLabel.getStyleClass().add("semester-title");
        Label descriptionLabel = new Label("Выберите семестр из списка");
        descriptionLabel.getStyleClass().add("semester-description");
        titleBox.getChildren().addAll(questionLabel, descriptionLabel);
        
        HBox comboBoxContainer = new HBox();
        comboBoxContainer.setAlignment(Pos.CENTER);
        comboBoxContainer.setMaxWidth(400);
        ComboBox<String> semesterComboBox = new ComboBox<>(); // Используем локальную переменную
        semesterComboBox.setPromptText("Выберите семестр");
        semesterComboBox.getItems().addAll("Семестр 1", "Семестр 2", "Семестр 3", "Семестр 4", "Семестр 5", "Семестр 6", "Семестр 7", "Семестр 8", "Семестр 9", "Семестр 10");
        semesterComboBox.setPrefWidth(400);
        semesterComboBox.getStyleClass().add("semester-combo-box");
        comboBoxContainer.getChildren().add(semesterComboBox);
        
        semesterSelectionPanel.getChildren().addAll(titleBox, comboBoxContainer);
        subjectsTilePane.getChildren().add(semesterSelectionPanel);
        
        semesterComboBox.setOnAction(event -> {
            selectedSemester = semesterComboBox.getValue();
            if (selectedSemester != null) {
                // Определяем номер семестра
                String semesterNumberStr = selectedSemester.replaceAll("[^0-9]", "");
                try {
                    int semester = Integer.parseInt(semesterNumberStr);
                    handleSemesterSelected(semester, semesterSelectionPanel); // Передаем панель для анимации
                } catch (NumberFormatException nfe) {
                    Logger.error("Не удалось определить номер семестра из: " + selectedSemester);
                }
            }
        });
        
        // Анимация появления панели выбора
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), semesterSelectionPanel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    /**
     * Обрабатывает выбор семестра и переключает на отображение предметов.
     * @param semester Выбранный номер семестра.
     * @param panelToFade Панель выбора семестра для анимации исчезновения.
     */
    private void handleSemesterSelected(Integer semester, VBox panelToFade) {
        ConfigManager.setValue("semester", semester.toString());
        Logger.info("Выбран семестр: " + semester);
        
        // Анимация исчезновения панели выбора семестра
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), panelToFade);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        
        fadeOut.setOnFinished(e -> {
            subjectsTilePane.getChildren().clear(); // Очищаем после анимации
            
            // Настраиваем основной вид
            setupMainView(); 
            
            // Загружаем данные и генерируем карточки
            initSubjects();
            generateSubjectCards(true); // Генерируем с анимацией
            
            // Инициализируем drag-and-drop
            dragAndDrop();
        });
        
        fadeOut.play();
    }

    /**
     * Генерирует карточки предметов для отображения.
     */
    private void generateSubjectCards(boolean isAnimated) {
        // Очищаем TilePane перед заполнением новыми элементами
        subjectsTilePane.getChildren().clear();
        
        // Проходим по массивам и создаем VBox для каждого предмета
        for (int i = 0; i < subjectNames.size(); i++) {
            VBox subjectCard = createSubjectCard(subjectNames.get(i), filesCount.get(i));
            subjectsTilePane.getChildren().add(subjectCard);
            if (isAnimated) {
                animationController.animateNewElementAppearance(subjectCard);
            }
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
     * Открывает страницу с содержимым выбранного предмета.
     * Метод выполняет следующие действия:
     * 1. Устанавливает текущий путь в ConfigManager
     * 2. Переключает контент на страницу предмета
     * 
     * @param subjectName Название предмета для открытия
     */
    private void openSubjectContent(String subjectName) {
        try {
            ConfigManager.setValue("path", subjectName + "/");
            
            // Вызываем статический метод с передачей данных предмета
            MainController.switchContent("subject-content.fxml");
            
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
            String command = dataString.indexOf('_') != -1 ?  dataString.substring(0, dataString.indexOf('_')) : dataString;
            String message = dataString.indexOf('_') != -1 ?  dataString.substring(dataString.indexOf('_') + 1) : "";
            if (command.equals("deleteFolder")) {
                // Удаляем предмет
                initSubjects();
                generateSubjectCards(false);
            } else if (command.equals("updateSubjects")) {
                // Обновляем список предметов
                initSubjects();
                generateSubjectCards(false);
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