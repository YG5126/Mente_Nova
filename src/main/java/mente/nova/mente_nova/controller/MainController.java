package mente.nova.mente_nova.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import mente.nova.mente_nova.view.Notification;
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

    @FXML
    private StackPane rootPane;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private static Notification notification;
    
    // Статическая ссылка на экземпляр контроллера
    private static MainController instance;
    
    /**
     * Инициализация контроллера. Сохраняет ссылку на экземпляр и загружает начальное содержимое.
     * @param location URL для инициализации
     * @param resources Ресурсы для инициализации
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        
        // Инициализируем notification
        notification = new Notification(rootPane);
        
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
     * Загружает содержимое из FXML файла в главную панель и передает данные в контроллер.
     * @param fxmlFile Имя FXML файла для загрузки
     * @param data Данные для передачи в контроллер
     */
    private void loadContent(String fxmlFile, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            loader.setControllerFactory(applicationContext::getBean);
            Node content = loader.load();
            
            // Получаем контроллер и передаем данные
            Object controller = loader.getController();
            if (controller instanceof DataReceiver) {
                ((DataReceiver) controller).receiveData(data);
            }
            
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
    
    /**
     * Статический метод для переключения содержимого главной панели с передачей данных.
     * @param fxmlFile Имя FXML файла для загрузки
     * @param data Данные для передачи в контроллер
     */
    public static void switchContent(String fxmlFile, Object data) {
        if (instance != null) {
            instance.loadContent(fxmlFile, data);
        } else {
            Logger.error("MainController не инициализирован");
        }
    }

    public static void showNotification(String type, String message) {
        if (notification != null) {
            switch(type) {
                case "success":
                    Logger.info(message);
                    notification.showSuccess(message);
                    break;
                case "error":
                    Logger.error(message);
                    notification.showError(message);
                    break;
                case "warning":
                    Logger.warn(message);
                    notification.showWarning(message);
                    break;
            }
        } else {
            Logger.error("Notification не инициализирован");
        }
    }
    
    /**
     * Интерфейс для контроллеров, которые могут получать данные при переключении сцены.
     */
    public interface DataReceiver {
        void receiveData(Object data);
    }
}