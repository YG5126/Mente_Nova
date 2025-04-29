package mente.nova.mente_nova;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import org.tinylog.Logger;

@SpringBootApplication  
public class MenteNovaApplication extends Application {

    private static String[] savedArgs;
    private static ConfigurableApplicationContext context;
    private static HostServices hostServices;

    public static void main(String[] args) {
        Logger.info("НАЧАЛО РАБОТЫ");
        savedArgs = args;
        launch(args);
    }    

    @Override
    public void init() throws Exception {
        hostServices = getHostServices();
        context = SpringApplication.run(MenteNovaApplication.class, savedArgs);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
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
        Logger.info("КОНЕЦ РАБОТЫ");
        System.exit(0);
    }

    public static HostServices getHostServicesInstance() {
        return hostServices;
    }
}
