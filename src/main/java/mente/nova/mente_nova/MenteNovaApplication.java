package mente.nova.mente_nova;

import java.io.IOException;

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

    public static void main(String[] args) {
        savedArgs = args;
        launch(args);
    }

    /*@Override
    public void run(String... args) throws Exception {
        System.out.println("Приложение запущено");
        //minio.createBucket("test");
        //minio.deleteFile("test", "задания.pdf");
        //minio.deleteFile("test", "задания2.pdf");
        //minio.loadingFile("test", "задания2.pdf", "C:/Users/Redmi G Pro/Desktop/Т-банк/задания2.pdf");
        //minio.loadingFile("test", "задания.pdf", "C:/Users/Redmi G Pro/Desktop/Т-банк/задания.pdf");
        //pdf.uploadPDF("test", "C:/Users/Redmi G Pro/Desktop/Т-банк/задания.pdf", "задания.pdf");
        //pdf.uploadPDF("test", "C:/Users/Redmi G Pro/Desktop/Т-банк/задания2.pdf", "задания2.pdf");
        //pdf.readPDF("test", "задания.pdf");
        //pdf.joinPDF("test", "задания.pdf", "задания2.pdf");
    }*/

    

    @Override
    public void init() throws Exception {
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
        System.exit(0);
    }
}
