package mente.nova.mente_nova.controller;

import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.util.Duration;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextField;
import javafx.scene.Node;

/**
 * Контроллер для управления анимациями элементов интерфейса.
 * Предоставляет методы для создания эффектов при наведении и взаимодействии с элементами.
 */
public class animationController {
    
    // Таймлайны для анимации поля поиска
    private static Timeline showSearchFieldTimeline;
    private static Timeline hideSearchFieldTimeline;
    
    /**
     * Настраивает анимацию подъема для VBox при наведении курсора.
     * Создает эффект поднятия элемента и изменения тени.
     * @param card VBox, для которого настраивается анимация
     */
    static void setupCardAnimationLifting(VBox card) {
        // Задаем начальный эффект тени
        DropShadow initialShadow = createInitialShadow();
        card.setEffect(initialShadow);
        
        // Создаем анимации
        Timeline hoverTimeline = createHoverTimeline(card, initialShadow);
        Timeline exitTimeline = createExitTimeline(card, initialShadow);
        
        // Добавляем обработчики событий
        card.setOnMouseEntered(_ -> hoverTimeline.play());
        card.setOnMouseExited(_ -> exitTimeline.play());
    }
    
    /**
     * Настраивает анимацию увеличения для HBox при наведении курсора.
     * Создает эффект плавного увеличения элемента.
     * @param card HBox, для которого настраивается анимация
     */
    static void setupCardAnimationIncrease(HBox card) {        
        // Создаем анимации увеличения вместо поднятия
        Timeline hoverTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(card.scaleXProperty(), 1.0),
                new KeyValue(card.scaleYProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(card.scaleXProperty(), 1.05),
                new KeyValue(card.scaleYProperty(), 1.05)
            )
        );
        
        // Анимация возврата к исходному размеру
        Timeline exitTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(card.scaleXProperty(), 1.05),
                new KeyValue(card.scaleYProperty(), 1.05)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(card.scaleXProperty(), 1.0),
                new KeyValue(card.scaleYProperty(), 1.0)
            )
        );
        
        // Добавляем обработчики событий
        card.setOnMouseEntered(_ -> hoverTimeline.play());
        card.setOnMouseExited(_ -> exitTimeline.play());
    }
    
    /**
     * Настраивает анимацию увеличения для Label при наведении курсора.
     * Создает эффект плавного увеличения текстового элемента.
     * @param label Label, для которого настраивается анимация
     */
    static void setupCardAnimationIncrease(Label label) {
        
        // Создаем анимации увеличения
        Timeline hoverTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(label.scaleXProperty(), 1.0),
                new KeyValue(label.scaleYProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(label.scaleXProperty(), 1.05),  // увеличение по ширине на 5%
                new KeyValue(label.scaleYProperty(), 1.05)  // увеличение по высоте на 5%
            )
        );
        
        // Анимация возврата к исходному размеру
        Timeline exitTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(label.scaleXProperty(), 1.05),
                new KeyValue(label.scaleYProperty(), 1.05)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(label.scaleXProperty(), 1.0),
                new KeyValue(label.scaleYProperty(), 1.0)
            )
        );
        
        // Добавляем обработчики событий
        label.setOnMouseEntered(_ -> hoverTimeline.play());
        label.setOnMouseExited(_ -> exitTimeline.play());
    }
    
    /**
     * Создает Timeline для анимации при наведении на карточку.
     * @param card Карточка для анимации
     * @param initialShadow Начальная тень
     * @return Timeline с анимацией наведения
     */
    private static Timeline createHoverTimeline(VBox card, DropShadow initialShadow) {
        // Конечный эффект тени (при наведении)
        DropShadow hoverShadow = createHoverShadow();
        
        // Создаем анимацию для наведения
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(card.translateYProperty(), 0),
                new KeyValue(card.effectProperty(), initialShadow)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(card.translateYProperty(), -5),
                new KeyValue(card.effectProperty(), hoverShadow)
            )
        );
        
        return timeline;
    }
    
    /**
     * Создает Timeline для анимации при уходе курсора с карточки.
     * @param card Карточка для анимации
     * @param initialShadow Начальная тень
     * @return Timeline с анимацией ухода
     */
    private static Timeline createExitTimeline(VBox card, DropShadow initialShadow) {
        // Конечный эффект тени (при наведении)
        DropShadow hoverShadow = createHoverShadow();
        
        // Создаем анимацию для выхода курсора
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(card.translateYProperty(), -5),
                new KeyValue(card.effectProperty(), hoverShadow)
            ),
            new KeyFrame(Duration.millis(300), 
                new KeyValue(card.translateYProperty(), 0),
                new KeyValue(card.effectProperty(), initialShadow)
            )
        );
        
        return timeline;
    }
    
    /**
     * Создает начальный эффект тени для элементов.
     * @return DropShadow с начальными параметрами
     */
    private static DropShadow createInitialShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(4);
        shadow.setRadius(10);
        shadow.setColor(javafx.scene.paint.Color.color(0, 0, 0, 0.1));
        return shadow;
    }
    
    /**
     * Создает эффект тени при наведении на элемент.
     * @return DropShadow с параметрами для наведения
     */
    private static DropShadow createHoverShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setOffsetY(8);
        shadow.setRadius(15);
        shadow.setColor(javafx.scene.paint.Color.color(0, 0, 0, 0.15));
        return shadow;
    }

    /**
     * Настраивает анимацию появления и исчезновения TextField.
     * @param textField TextField, который будет анимироваться
     */
    public static void setupSearchFieldAnimation(TextField textField) {
        // Изначально поле поиска скрыто
        textField.setOpacity(0);
        textField.setPrefWidth(0);
        textField.setVisible(false);
        
        // Анимация появления
        showSearchFieldTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                e -> textField.setVisible(true),
                new KeyValue(textField.opacityProperty(), 0),
                new KeyValue(textField.prefWidthProperty(), 0)
            ),
            new KeyFrame(Duration.millis(350), 
                new KeyValue(textField.opacityProperty(), 1),
                new KeyValue(textField.prefWidthProperty(), 200)
            )
        );
        
        // Анимация исчезновения
        hideSearchFieldTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(textField.opacityProperty(), 1),
                new KeyValue(textField.prefWidthProperty(), 200)
            ),
            new KeyFrame(Duration.millis(350), 
                new KeyValue(textField.opacityProperty(), 0),
                new KeyValue(textField.prefWidthProperty(), 0)
            )
        );
        hideSearchFieldTimeline.setOnFinished(e -> textField.setVisible(false));
    }
    
    /**
     * Показывает TextField с анимацией
     * @param textField TextField для отображения
     */
    public static void showSearchField(TextField textField) {
        if (showSearchFieldTimeline == null) {
            setupSearchFieldAnimation(textField);
        };
        hideSearchFieldTimeline.stop();
        showSearchFieldTimeline.play();
        textField.requestFocus();
    }
    
    /**
     * Скрывает TextField с анимацией
     * @param textField TextField для скрытия
     */
    public static void hideSearchField(TextField textField) {
        if (hideSearchFieldTimeline == null) {
            setupSearchFieldAnimation(textField);
        }
        showSearchFieldTimeline.stop();
        hideSearchFieldTimeline.play();
    }

    /**
     * Анимирует появление нового элемента в контейнере
     * @param node Элемент для анимации
     */
    public static void animateNewElementAppearance(Node node) {
        // Начальные параметры - элемент невидим и уменьшен
        node.setOpacity(0);
        node.setScaleX(0.5);
        node.setScaleY(0.5);
        node.setTranslateY(20);
        
        // Создаем анимацию появления
        Timeline appearTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(node.opacityProperty(), 0),
                new KeyValue(node.scaleXProperty(), 0.5),
                new KeyValue(node.scaleYProperty(), 0.5),
                new KeyValue(node.translateYProperty(), 20)
            ),
            new KeyFrame(Duration.millis(500), 
                new KeyValue(node.opacityProperty(), 1),
                new KeyValue(node.scaleXProperty(), 1),
                new KeyValue(node.scaleYProperty(), 1),
                new KeyValue(node.translateYProperty(), 0)
            )
        );
        
        // Запускаем анимацию
        appearTimeline.play();
    }

}
