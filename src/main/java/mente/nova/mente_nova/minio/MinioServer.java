package mente.nova.mente_nova.minio;

import org.springframework.context.annotation.Configuration;

import java.net.Socket;
import java.io.IOException;

@Configuration
public class MinioServer {

    public static void startServer() {
        try {

            if (isPortInUse(9000)) {
                System.out.println("Ошибка: Порт 9000 уже занят");
                stopServer();
            }

            //("cmd.exe", "/c", "src/main/resources/minio.exe server C:/Users/PC/Projects/mente_nova/src/main/resources/server")
            System.out.println("Запуск MinIO сервера...");
            ProcessBuilder processBuilderStart = new ProcessBuilder("src/main/resources/nova.bat");
            processBuilderStart.redirectErrorStream(true);
            processBuilderStart.start();

            System.out.println("Ожидание запуска MinIO сервера...");
            while (!isPortInUse(9000)) {
                //Ожидание...
            }
            
            System.out.println("MinIO сервер запущен");

        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    public static void stopServer() {
        try {
            
            System.out.println("Остановка MinIO сервера...");
            ProcessBuilder processBuilderStop = new ProcessBuilder("cmd.exe", "/C", "taskkill /F /IM minio.exe /T");
            processBuilderStop.redirectErrorStream(true);
            processBuilderStop.start();

            System.out.println("Ожидание остановки MinIO сервера...");
            while (isPortInUse(9000)) {
                //Ожидание...
            }
            
            System.out.println("MinIO сервер остановлен");

        } catch (IOException e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    public static boolean isPortInUse(int port) {
        try (Socket _ = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
