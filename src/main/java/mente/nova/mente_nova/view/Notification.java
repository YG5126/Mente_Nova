package mente.nova.mente_nova.view;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class Notification {
    private final Timeline fadeOutTimeline;
    private final VBox notificationBox;
    private final StackPane wrapper;

    /**
     * Создает новый компонент уведомления, прикрепленный к указанному rootStackPane.
     * Настраивает внешний вид, позиционирование и анимацию уведомления.
     * 
     * @param rootStackPane корневой контейнер для размещения уведомлений
     */
    public Notification(StackPane rootStackPane) {        
        // Создаем контейнер для уведомления
        this.notificationBox = new VBox(10);
        notificationBox.setAlignment(Pos.CENTER_LEFT);
        notificationBox.setPadding(new Insets(20));
        notificationBox.setMaxWidth(340); // Ограничение максимальной ширины (300 + 20 + 20)
        notificationBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        notificationBox.setOpacity(0);
        
        // Упакуем VBox в контейнер-обертку
        wrapper = new StackPane(notificationBox);
        wrapper.setVisible(false);
        wrapper.setMouseTransparent(true); // Чтобы не блокировать события мыши
        wrapper.setPickOnBounds(false); // Не перехватывать события в пустой области
        wrapper.setAlignment(Pos.TOP_RIGHT);
        wrapper.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        wrapper.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        wrapper.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        
        // Устанавливаем позиционирование wrapper в корневом StackPane (справа вверху)
        StackPane.setAlignment(wrapper, Pos.TOP_RIGHT);
        StackPane.setMargin(wrapper, new Insets(30, 30, 0, 0));
        
        // Добавляем wrapper в корневой StackPane
        rootStackPane.getChildren().add(wrapper);
        
        // Настраиваем анимацию исчезновения
        fadeOutTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(notificationBox.opacityProperty(), 1)),
            new KeyFrame(Duration.seconds(0.5), new KeyValue(notificationBox.opacityProperty(), 0))
        );
        fadeOutTimeline.setOnFinished(_ -> {
            wrapper.setVisible(false);
            wrapper.setMouseTransparent(true); // Возвращаем прозрачность для мыши
        });
    }
    
    /**
     * Показывает уведомление с указанными параметрами.
     * Создает заголовок и текст сообщения, применяет стили и запускает таймер для автоматического скрытия.
     *
     * @param title заголовок уведомления
     * @param message основной текст уведомления
     * @param backgroundColor цвет фона в формате HEX (например, "#EDFAF1")
     * @param borderColor цвет границы в формате HEX (например, "#34C759")
     * @param titleColor цвет заголовка в формате HEX (например, "#34C759")
     */
    public void show(String title, String message, String backgroundColor, String borderColor, String titleColor) {
        // Останавливаем текущую анимацию, если она выполняется
        fadeOutTimeline.stop();
        notificationBox.setOpacity(1); // Делаем видимым внутренний контейнер перед анимацией
        wrapper.setVisible(true);
        wrapper.setMouseTransparent(false); // Активируем перехват событий во время показа
        
        // Очищаем предыдущее содержимое
        notificationBox.getChildren().clear();
        
        // Создаем заголовок
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        titleLabel.setFont(new Font(18));
        
        // Создаем основное сообщение
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: #333333;");
        messageLabel.setFont(new Font(16));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300); // Максимальная ширина текста перед переносом
        
        // Добавляем элементы в контейнер
        notificationBox.getChildren().addAll(titleLabel, messageLabel);
        
        // Стилизация контейнера
        notificationBox.setStyle(
            "-fx-background-color: " + backgroundColor + "; " +
            "-fx-border-color: " + borderColor + "; " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8;"
        );
        
        // Запускаем таймер для автоматического скрытия
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            fadeOutTimeline.play();
        });
        delay.play();
    }
    
    /**
     * Показывает уведомление об успешном действии с зеленой цветовой схемой.
     *
     * @param message текст уведомления
     */
    public void showSuccess(String message) {
        show("Успешно", message, "#EDFAF1", "#34C759", "#34C759");
    }
    
    /**
     * Показывает уведомление об ошибке с красной цветовой схемой.
     *
     * @param message текст уведомления
     */
    public void showError(String message) {
        show("Ошибка", message, "#FFF1F0", "#FF3B30", "#FF3B30");
    }
    
    /**
     * Показывает предупреждающее уведомление с оранжевой цветовой схемой.
     *
     * @param message текст уведомления
     */
    public void showWarning(String message) {
        show("Предупреждение", message, "#FFF9ED", "#FF9500", "#FF9500");
    }
}