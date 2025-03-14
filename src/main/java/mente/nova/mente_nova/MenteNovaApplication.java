package mente.nova.mente_nova;

import mente.nova.mente_nova.minio.*;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

@SpringBootApplication  
public class MenteNovaApplication extends Application {
    private static String[] savedArgs;
    private ConfigurableApplicationContext context;

    @Autowired
    private MinioApplication minio;

    public static void main(String[] args) {
        savedArgs = args;
        launch(args);
    }

    @Override
    public void init() throws Exception {
        context = SpringApplication.run(MenteNovaApplication.class, savedArgs);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Mente-Nova");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        primaryStage.setTitle("Mente-Nova");
        primaryStage.setMinHeight(600);
        primaryStage.setMinWidth(1000);
        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        context.close();
        super.stop();
    }
}
