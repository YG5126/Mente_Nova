package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import mente.nova.mente_nova.minio.MinioApplication;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Controller;
import org.tinylog.Logger;

/**
 * Главный контроллер приложения, управляющий основным интерфейсом.
 * Отвечает за загрузку и переключение содержимого главной панели.
 */
@Controller
public class MainController implements Initializable {
    
    @FXML
    private VBox mainPanel;
    
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MinioApplication minio;
    
    // Статическая ссылка на экземпляр контроллера
    private static MainController instance;
    
    /**
     * Инициализация контроллера. Сохраняет ссылку на экземпляр и загружает начальное содержимое.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        //System.out.println(minio.returnFile("1 семестр/Английский язык/Unit 2 (2.1 - 2.3).docx").getName());
        //minio.lastFileChanges(minio.returnFile("1 семестр/Английский язык/Unit 2 (2.1 - 2.3).docx"));
        minio.fileMoving("1 семестр/Английский язык/Unit 2 (2.1 - 2.3).docx", "1 семестр/Информатика/Unit 2 (2.1 - 2.3).docx");
        //minio.sizeReturn(minio.returnFile("1 семестр/Английский язык/Unit 2 (2.1 - 2.3).docx"));
        // Сохраняем ссылку на экземпляр
        instance = this;
        
        // Загружаем начальное содержимое
        loadContent("main-content.fxml");
    }
    
    /**
     * Загружает содержимое из FXML файла в главную панель.
     * @param fxmlFile Имя FXML файла для загрузки
     */
    private void loadContent(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            loader.setControllerFactory(applicationContext::getBean);
            Node content = loader.load();
            
            mainPanel.getChildren().clear();
            mainPanel.getChildren().add(content);
            
        } catch (IOException e) {
            Logger.error("Ошибка при загрузке содержимого: " + e.getMessage());
        }
    }
    
    /**
     * Статический метод для переключения содержимого главной панели.
     * @param fxmlFile Имя FXML файла для загрузки
     */
    public static void switchContent(String fxmlFile) {
        if (instance != null) {
            instance.loadContent(fxmlFile);
        } else {
            Logger.error("MainController не инициализирован");
        }
    }
}