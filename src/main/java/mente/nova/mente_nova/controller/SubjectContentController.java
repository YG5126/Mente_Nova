package mente.nova.mente_nova.controller;


import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.MinioApplication;
import mente.nova.mente_nova.minio.MinioList.Node;
import mente.nova.mente_nova.pdf.pdfApplication;
import mente.nova.mente_nova.pdf.pdfApplication.PdfPageData;

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

    @FXML
    private HBox menu;

    @FXML
    private StackPane sectionMenu;
    
    @Autowired
    private MinioApplication minio;

    @Autowired
    private pdfApplication pdf;
    
    /**
     * Инициализация контроллера. Настраивает начальное состояние и загружает содержимое.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String path = ConfigManager.getValue("path");
        backButton.setOnAction(_ -> goBack());
        createPathContainer(path);
        
        generateActionMenu();

        if (path.lastIndexOf("/") == path.length() - 1) {
            path = path.substring(0, path.length() - 1);
        }

        setSubjectName(path.substring(path.lastIndexOf("/") + 1, path.length()));
    }

    public void generateActionMenu() {
        sectionMenu.getChildren().clear();
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/image/action_menu.png")));
        sectionMenu.getChildren().add(imageView);
        sectionMenu.getStyleClass().add("sectionMenu");

        // Создаем контекстное меню
        ContextMenu contextMenu = new ContextMenu();
        
        // Добавляем пункты меню
        MenuItem item1 = new MenuItem("Удалить предмет");
        MenuItem item2 = new MenuItem("Загрузить файл");
        MenuItem item3 = new MenuItem("Загрузить папку");

        // Обработчики для пунктов меню
        item1.setOnAction(e -> {
            minio.annihilateFolder(ConfigManager.getValue("path"));
            MainController.switchContent("section-content.fxml", "deleteFolder_" + ConfigManager.getValue("path"));
        });
        item2.setOnAction(e -> System.out.println("Удалить изображение!"));
        item3.setOnAction(e -> System.out.println("Свойства изображения!"));

        contextMenu.getItems().addAll(item1, item2, item3);

        sectionMenu.setOnMouseClicked(event -> {
            contextMenu.show(
                sectionMenu, 
                event.getScreenX(), 
                event.getScreenY()
            );
        });
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
        homeContainer.setOnMouseClicked(_ -> openSubjectCard("", true));

        pathItem.getChildren().add(homeContainer);
        pathItem.getChildren().add(new ImageView(new Image(getClass().getResourceAsStream("/image/arrow_path.png"))));
        String currentPath = "";
        for (int i = 0; i < pathParts.length; i++) {
            currentPath += pathParts[i] + "/";
            Label pathLabel = new Label(pathParts[i]);
            if (i < pathParts.length - 1) {
                animationController.setupCardAnimationIncrease(pathLabel);
                final String finalPath = currentPath;
                pathLabel.setOnMouseClicked(_ -> openSubjectCard(finalPath, true));
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
                loadPDFFileContent(fileName);
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
            Node list = minio.list(ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path"), false);
            
            for (Node subjectName : list.getChildren().values()) {
                HBox subjectCard = createFileCard(subjectName.getName(), subjectName.isDirectory());
                contentContainer.getChildren().add(subjectCard);
            }
            
        } catch (Exception e) {
            Logger.error("Ошибка при загрузке содержимого предмета: " + e.getMessage());
        }
    }

    /**
     * Загружает PDF файл и отображает его страницы с возможностью выделения текста
     * @param fileName Имя PDF файла
     */
    private void loadPDFFileContent(String fileName) {
        try {
            // Очистка контейнера
            contentContainer.getChildren().clear();
            
            // Показываем индикатор загрузки
            ProgressIndicator progress = new ProgressIndicator();
            progress.setPrefSize(50, 50);
            VBox loadingBox = new VBox(progress, new Label("Загрузка PDF..."));
            loadingBox.getStyleClass().add("loading-box");
            contentContainer.getChildren().add(loadingBox);
            VBox.setVgrow(loadingBox, Priority.ALWAYS);
            
            // Создаем и запускаем задачу для загрузки PDF в фоновом потоке
            Task<List<PdfPageData>> renderTask = new Task<>() {
                @Override
                protected List<PdfPageData> call() throws Exception {
                    return pdf.readPDF(ConfigManager.getValue("path"));
                }
            };
            
            renderTask.setOnSucceeded(_ -> {

                StackPane homeIcon = new StackPane(new ImageView(new Image(getClass().getResourceAsStream("/image/download_pdf_icon.png"))));
                homeIcon.setOnMouseClicked(_ -> {
                    String serverPath = ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path");
                    File pdfFile = minio.returnFile(serverPath);

                    if (pdfFile != null && pdfFile.exists()) {
                        // Получаем Stage из компонента homeIcon
                        Stage stage = (Stage) homeIcon.getScene().getWindow();
                        
                        // Создаем диалог сохранения файла
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Mente Nova - Сохранение PDF файла");
                        
                        // Устанавливаем имя файла по умолчанию
                        String pdfName = pdfFile.getName();
                        fileChooser.setInitialFileName(pdfName);
                        
                        // Добавляем фильтр файлов PDF
                        FileChooser.ExtensionFilter extFilter =
                            new FileChooser.ExtensionFilter("PDF файлы (*.pdf)", "*.pdf");
                        fileChooser.getExtensionFilters().add(extFilter);
                        
                        // Показываем диалог сохранения
                        File saveFile = fileChooser.showSaveDialog(stage);
                        
                        if (saveFile != null) {
                            try {
                                // Копируем временный файл в выбранное место
                                Files.copy(pdfFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                Logger.info("Файл успешно сохранен: " + saveFile.getAbsolutePath());
                            } catch (IOException e) {
                                Logger.error("Ошибка при сохранении файла: " + e.getMessage());
                                // Показываем сообщение об ошибке пользователю
                                Label errorLabel = new Label("Ошибка при сохранении файла: " + e.getMessage());
                                errorLabel.getStyleClass().add("error-message");
                                contentContainer.getChildren().add(errorLabel);
                            }
                        }
                    } else {
                        Logger.error("Ошибка: Файл не был загружен с сервера");
                        Label errorLabel = new Label("Ошибка: Файл не был загружен с сервера");
                        errorLabel.getStyleClass().add("error-message");
                        contentContainer.getChildren().add(errorLabel);
                    }
                });
                menu.getChildren().add(homeIcon);


                List<PdfPageData> pagesData = renderTask.getValue();
                
                // Выполняем обработку всех изображений страниц сразу в фоновом потоке
                Task<List<Image>> processImagesTask = new Task<>() {
                    @Override
                    protected List<Image> call() throws Exception {
                        List<Image> images = new ArrayList<>();
                        for (PdfPageData pageData : pagesData) {
                            Image fxImage = convertToFxImage(pageData.getImage());
                            images.add(fxImage);
                            // Освобождаем ресурсы BufferedImage после конвертации
                            if (pageData.getImage() != null) {
                                pageData.getImage().flush();
                            }
                            // Запускаем сборщик мусора для освобождения памяти
                            System.gc();
                        }
                        return images;
                    }
                };
                
                processImagesTask.setOnSucceeded(_ -> {

                    List<Image> images = processImagesTask.getValue();
                    
                    Platform.runLater(() -> {
                        contentContainer.getChildren().clear();
                        
                        if (images.isEmpty() || pagesData.isEmpty()) {
                            Logger.error("Не удалось загрузить PDF файл.");
                            Label errorLabel = new Label("Не удалось загрузить PDF файл.");
                            errorLabel.getStyleClass().add("error-message");
                            contentContainer.getChildren().add(errorLabel);
                            return;
                        }
                        
                        // Создаем ScrollPane с оптимизированной производительностью
                        ScrollPane scrollPane = new ScrollPane();
                        scrollPane.getStyleClass().add("pdf-scroll-pane");
                        scrollPane.setFitToWidth(true);
                        scrollPane.setCache(true);
                        
                        // Создаем контейнер для страниц с фиксированной шириной
                        VBox pagesContainer = new VBox();
                        pagesContainer.getStyleClass().add("pdf-pages-container");
                        
                        // Добавляем все страницы сразу
                        for (int i = 0; i < images.size(); i++) {
                            // Создаем контейнер страницы
                            VBox pageBox = new VBox();
                            pageBox.getStyleClass().add("pdf-page");
                            
                            // Заголовок страницы
                            Label pageHeader = new Label("Страница " + (i + 1));
                            pageHeader.getStyleClass().add("pdf-page-header");
                            
                            // Создаем ImageView с оптимизированными настройками
                            ImageView imageView = new ImageView(images.get(i));
                            imageView.setCache(true);
                            imageView.setCacheHint(javafx.scene.CacheHint.SPEED);
                            imageView.getStyleClass().add("pdf-image");
                            
                            // Добавляем элементы на страницу
                            pageBox.getChildren().addAll(pageHeader, imageView);
                            pagesContainer.getChildren().add(pageBox);
                        }
                        
                        scrollPane.setContent(pagesContainer);
                        contentContainer.getChildren().add(scrollPane);
                        VBox.setVgrow(scrollPane, Priority.ALWAYS);
                    });
                });
                
                // Обработка ошибок при обработке изображений
                processImagesTask.setOnFailed(_ -> {
                    Platform.runLater(() -> {
                        Logger.error("Ошибка при обработке PDF: " + processImagesTask.getException().getMessage());
                        contentContainer.getChildren().clear();
                        Label errorLabel = new Label("Ошибка при обработке PDF: " + processImagesTask.getException().getMessage());
                        errorLabel.getStyleClass().add("error-message");
                        contentContainer.getChildren().add(errorLabel);
                    });
                });
                
                // Запускаем задачу обработки изображений
                new Thread(processImagesTask).start();
            });
            
            renderTask.setOnFailed(_ -> {
                Platform.runLater(() -> {
                    Logger.error("Ошибка при загрузке PDF файла: " + renderTask.getException().getMessage());
                    contentContainer.getChildren().clear();
                    Label errorLabel = new Label("Ошибка загрузки PDF: " + renderTask.getException().getMessage());
                    errorLabel.getStyleClass().add("error-message");
                    contentContainer.getChildren().add(errorLabel);
                });
            });
            
            // Запускаем задачу в отдельном потоке
            new Thread(renderTask).start();
            
        } catch (Exception e) {
            Logger.error("Ошибка при загрузке PDF файла: " + e.getMessage());
            contentContainer.getChildren().clear();
            Label errorLabel = new Label("Ошибка: " + e.getMessage());
            errorLabel.getStyleClass().add("error-message");
            contentContainer.getChildren().add(errorLabel);
        }
    }
    
    /**
     * Преобразует BufferedImage в JavaFX Image
     * @param bufferedImage Изображение для конвертации
     * @return JavaFX Image
     */
    private Image convertToFxImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        
        WritableImage writableImage = new WritableImage(
                bufferedImage.getWidth(), 
                bufferedImage.getHeight());
        
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        
        for (int x = 0; x < bufferedImage.getWidth(); x++) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }
        
        return writableImage;
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

        Label fileWeight = new Label("Последнее изменение: 00:00 · 1000МБ");
        fileWeight.getStyleClass().add("description");

        cardStructure.getChildren().addAll(fileNameLabel, fileWeight);

        subjectCard.getChildren().addAll(imageContainer, cardStructure, arrowImage);

        subjectCard.setOnMouseClicked(_ -> openSubjectCard(ConfigManager.getValue("path") + (ConfigManager.getValue("path").lastIndexOf("/") != -1 ? "" : "/") + fileName, isDirectory));

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
                MainController.switchContent("section-content.fxml", "updateSubjects_");
            } else {
                MainController.switchContent("subject-content.fxml");
            }
            
        } catch (Exception e) {
            Logger.error("Ошибка при открытии страницы предмета: " + e.getMessage());
        }
    }

}