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
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.image.BufferedImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.controller.MainController.DataReceiver;
import mente.nova.mente_nova.minio.MinioApplication;
import mente.nova.mente_nova.minio.MinioList.Node;
import mente.nova.mente_nova.pdf.pdfApplication;
import mente.nova.mente_nova.pdf.pdfApplication.PdfPageData;

/**
 * Контроллер для отображения содержимого предмета и навигации по файловой структуре.
 * Управляет отображением файлов, папок и навигационной панели.
 */
@Component
public class SubjectContentController implements Initializable, DataReceiver {

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

    private boolean isFolder;
    private Thread loadPdf;
    private Task<List<PdfPageData>> renderTask;
    private volatile boolean cancelPdfLoading = false;
    
    /**
     * Инициализация контроллера. Настраивает начальное состояние и загружает содержимое.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String path = ConfigManager.getValue("path");
        backButton.setOnAction(_ -> goBack(false));
        createPathContainer(path);

        if (path.lastIndexOf("/") == path.length() - 1) {
            path = path.substring(0, path.length() - 1);
        }

        setSubjectName(path.substring(path.lastIndexOf("/") + 1, path.length()));
        generateActionMenu();
    }

    /**
     * Инициализация меню действий.
     */
    public void generateActionMenu() {
        sectionMenu.getChildren().clear();
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/image/action_menu.png")));
        sectionMenu.getChildren().add(imageView);
        sectionMenu.getStyleClass().add("sectionMenu");

        // Создаем контекстное меню
        ContextMenu contextMenu = new ContextMenu();

        if (isFolder) {
            MenuItem item_delete = new MenuItem("Удалить папку");
            MenuItem item_load_file = new MenuItem("Загрузить файлы");
            MenuItem item_load_folder = new MenuItem("Загрузить папку");
            if (ConfigManager.getValue("path").indexOf('/') == ConfigManager.getValue("path").lastIndexOf('/')) {
                item_delete.setText("Удалить предмет");
            }

            item_delete.setOnAction(_ -> {
                minio.annihilateFolder(ConfigManager.getValue("path"));
                goBack(true);
            });
            item_load_file.setOnAction(_ -> {
                String folderPath = ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path");

                // Создаем диалог выбора файла
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Mente Nova - Загрузка файла(ов)");

                // Добавляем фильтры для различных типов файлов
                FileChooser.ExtensionFilter allFilter =
                    new FileChooser.ExtensionFilter("Все файлы", "*.*");
                FileChooser.ExtensionFilter pdfFilter =
                    new FileChooser.ExtensionFilter("PDF файлы (*.pdf)", "*.pdf");
                FileChooser.ExtensionFilter docFilter =
                    new FileChooser.ExtensionFilter("Документы Word (*.doc, *.docx)", "*.doc", "*.docx");
                FileChooser.ExtensionFilter odtFilter =
                    new FileChooser.ExtensionFilter("Документы Writer (*.odt)", "*.odt");

                fileChooser.getExtensionFilters().addAll(
                    allFilter, pdfFilter, docFilter, odtFilter
                );

                // Получаем Stage из любого компонента сцены
                Stage stage = (Stage) subjectTitle.getScene().getWindow();

                // Показываем диалог и получаем СПИСОК выбранных файлов
                List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

                if (selectedFiles != null && !selectedFiles.isEmpty()) {
                    int successCount = 0;
                    for (File selectedFile : selectedFiles) {
                        if (selectedFile != null) {
                            // Сохраняем путь к выбранному файлу
                            String localFilePath = selectedFile.getAbsolutePath();
                            Logger.info("Выбран файл: " + localFilePath);
                            // Формируем путь на сервере
                            String serverFilePath = folderPath + selectedFile.getName();
                            // Загружаем файл
                            minio.loadingFile(serverFilePath, localFilePath);
                            // Создаем и добавляем карточку файла
                            HBox subjectCard = createFileCard(selectedFile.getName(), false);
                            animationController.animateNewElementAppearance(subjectCard);
                            contentContainer.getChildren().add(subjectCard);
                            successCount++;
                        }
                    }
                    // Показываем общее уведомление
                    if (successCount > 0) {
                         MainController.showNotification("success", "Загружено файлов: " + successCount);
                    } else {
                         MainController.showNotification("error", "Не удалось загрузить выбранные файлы");
                    }
                } else {
                    Logger.info("Выбор файла(ов) отменен");
                }
            });
            item_load_folder.setOnAction(_ -> {
                String folderPath = ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path");

                // Создаем диалог выбора папки (выбор только ОДНОЙ папки)
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Mente Nova - Загрузка папки");

                // Получаем Stage из любого компонента сцены
                Stage stage = (Stage) subjectTitle.getScene().getWindow();

                // Показываем диалог и получаем выбранную папку
                File selectedFolder = directoryChooser.showDialog(stage);

                if (selectedFolder != null) {
                    // Сохраняем путь к выбранной папке
                    String localFolderPath = selectedFolder.getAbsolutePath();
                    Logger.info("Выбрана папка: " + localFolderPath);

                    // Загружаем папку в MinIO
                    minio.loadingFolder(folderPath + selectedFolder.getName() + "/", localFolderPath);

                    // Обновляем содержимое
                    contentContainer.getChildren().clear();
                    loadSubjectContent();
                } else {
                    Logger.info("Выбор папки отменен");
                }
            });
    
            contextMenu.getItems().addAll(item_delete, item_load_file, item_load_folder);
        } else {
            MenuItem item_delete_file = new MenuItem("Удалить файл");
            item_delete_file.setOnAction(_ -> {
                minio.deleteFile(ConfigManager.getValue("semester") + " семестр/" + ConfigManager.getValue("path"));
                goBack(true);
            });
            contextMenu.getItems().add(item_delete_file);
        }        

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
        this.isFolder = path.lastIndexOf("/")  == path.length()-1;
        
        if (isFolder) {
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
            
            // Сбрасываем флаг отмены при начале новой загрузки
            cancelPdfLoading = false;
            
            // Показываем индикатор загрузки
            ProgressIndicator progress = new ProgressIndicator();
            progress.setPrefSize(50, 50);
            VBox loadingBox = new VBox(progress, new Label("Загрузка PDF..."));
            loadingBox.getStyleClass().add("loading-box");
            contentContainer.getChildren().add(loadingBox);
            VBox.setVgrow(loadingBox, Priority.ALWAYS);
            
            // Создаем и запускаем задачу для загрузки PDF в фоновом потоке
            renderTask = new Task<List<PdfPageData>>() {
                @Override
                protected List<PdfPageData> call() throws Exception {
                    try {
                        // Периодически проверяем флаг отмены
                        if (cancelPdfLoading) {
                            Logger.info("Задача загрузки PDF отменена перед выполнением");
                            return new ArrayList<>();
                        }
                        
                        // Во время выполнения задачи добавляем свой обработчик прерывания
                        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                            Logger.error("Ошибка в потоке загрузки PDF: " + throwable.getMessage());
                        });
                        
                        return pdf.readPDF(ConfigManager.getValue("path"));
                    } catch (Exception e) {
                        if (cancelPdfLoading) {
                            Logger.info("Задача загрузки PDF отменена во время выполнения");
                            return new ArrayList<>();
                        }
                        Logger.error("Ошибка при выполнении задачи загрузки PDF: " + e.getMessage());
                        throw e;
                    }
                }
            };
            
            renderTask.setOnSucceeded(_ -> {
                try {
                    if (cancelPdfLoading) {
                        Logger.info("Задача загрузки PDF была отменена, пропускаем обработку результата");
                        return;
                    }

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
                            try {
                                for (PdfPageData pageData : pagesData) {
                                    // Проверяем флаг отмены во время обработки изображений
                                    if (cancelPdfLoading) {
                                        Logger.info("Обработка изображений PDF отменена");
                                        // Освобождаем ресурсы
                                        for (PdfPageData pd : pagesData) {
                                            if (pd.getImage() != null) {
                                                pd.getImage().flush();
                                            }
                                        }
                                        System.gc();
                                        return images;
                                    }
                                    
                                    Image fxImage = convertToFxImage(pageData.getImage());
                                    images.add(fxImage);
                                    // Освобождаем ресурсы BufferedImage после конвертации
                                    if (pageData.getImage() != null) {
                                        pageData.getImage().flush();
                                    }
                                    // Запускаем сборщик мусора для освобождения памяти
                                    System.gc();
                                }
                            } catch (Exception e) {
                                Logger.error("Ошибка при обработке изображений PDF: " + e.getMessage());
                                if (cancelPdfLoading) {
                                    return images;
                                }
                                throw e;
                            }
                            return images;
                        }
                    };
                    
                    processImagesTask.setOnSucceeded(_ -> {
                        // Проверяем флаг отмены перед обработкой результата
                        if (cancelPdfLoading) {
                            Logger.info("Обработка изображений была отменена, пропускаем отображение");
                            return;
                        }

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
                } catch (Exception e) {
                    Logger.error("Ошибка при обработке результатов PDF: " + e.getMessage());
                    Platform.runLater(() -> {
                        contentContainer.getChildren().clear();
                        Label errorLabel = new Label("Ошибка: " + e.getMessage());
                        errorLabel.getStyleClass().add("error-message");
                        contentContainer.getChildren().add(errorLabel);
                    });
                }
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
            loadPdf = new Thread(renderTask);
            loadPdf.start();
            
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
        boolean isunknownFile = false;
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

        cardStructure.getChildren().addAll(fileNameLabel);

        if (!isDirectory) {
            File current = minio.returnFile(ConfigManager.getValue("semester") + " семестр/" +  ConfigManager.getValue("path") + fileName);
            long size = minio.sizeReturn(current);
            StringBuffer lastChanges;
            if (size == -1) {
                lastChanges = new StringBuffer("Формат файла не поддерживается");
                isunknownFile = true;
            } else {
                HashMap<String, Integer> fileChanges = minio.lastChanges(current);
                int volumeDepth = 0;
                String[] listSizes = {"Б", "КБ", "МБ", "ГБ", "ТБ"};
                lastChanges = new StringBuffer("Последнее изменение: ");
                lastChanges.append(fileChanges.get("час") + ":" + fileChanges.get("минута") + " · ");
                while (size/1024!=0) {
                    volumeDepth++;
                    size/=1024;
                }
                lastChanges.append(size + " " + listSizes[volumeDepth]);
            }
            Label fileWeight = new Label(lastChanges.toString());
            fileWeight.getStyleClass().add("description");
            cardStructure.getChildren().add(fileWeight);
        }

        subjectCard.getChildren().addAll(imageContainer, cardStructure, arrowImage);

        if (!isunknownFile) {
            subjectCard.setOnMouseClicked(_ -> openSubjectCard(ConfigManager.getValue("path") + (ConfigManager.getValue("path").lastIndexOf("/") != -1 ? "" : "/") + fileName, isDirectory));
        }

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
            MainController.switchContent("section-content.fxml", "updateSubjects");
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
    private void goBack(boolean isUpdate) {
        
        try {
            // Устанавливаем флаг отмены перед отменой задачи
            cancelPdfLoading = true;
            
            if (renderTask != null && renderTask.isRunning()) {
                renderTask.cancel(false); // Используем false, чтобы избежать прерывания потока
            }

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
                if (isUpdate) {
                    MainController.switchContent("subject-content.fxml", "updateSubjects");
                } else {
                    MainController.switchContent("subject-content.fxml");
                }
            }
            
        } catch (Exception e) {
            Logger.error("Ошибка при открытии страницы предмета: " + e.getMessage());
        }
    }

    @Override
    public void receiveData(Object data) {
        Logger.info("Получены данные в SectionController: " + data);
        
        // Обрабатываем полученные данные в зависимости от их типа
        if (data instanceof String) {
            String dataString = (String) data;
            String command = dataString.indexOf('_') != -1 ?  dataString.substring(0, dataString.indexOf('_')) : dataString;
            String message = dataString.indexOf('_') != -1 ?  dataString.substring(dataString.indexOf('_') + 1) : "";
            if (command.equals("updateSubjects")) {
                // Обновляем список предметов
                contentContainer.getChildren().clear();
                loadSubjectContent();
            } else {
                Logger.error("Полученная команда неизвестна: " + command);
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