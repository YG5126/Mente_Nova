package mente.nova.mente_nova.minio;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

import java.net.Socket;
import java.io.IOException;

@Configuration
public class MinioServer {

    //Запуск сервера MinIO
    public static void startServer() {
        try {

            if (isPortInUse(9000)) {
                System.out.println("Ошибка: Порт 9000 уже занят");
                stopServer();
            }

            //Запуск сервера с поомщью ProcessBuilder и bat-скрипта запуска MinIO
            System.out.println("Запуск MinIO сервера...");
            ProcessBuilder processBuilderStart = new ProcessBuilder("src/main/resources/nova.bat");
            //Перенаправление вывода ошибок и результатов на стандартные потоки
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


    @PreDestroy
    //Остановка сервера
    public static void stopServer() {
        try {
            
            //Остановка сервера с помощью ProcessBuilder и завершения всех процессов по имени minio.exe
            System.out.println("Остановка MinIO сервера...");
            ProcessBuilder processBuilderStop = new ProcessBuilder("cmd.exe", "/C", "taskkill /F /IM minio.exe /T");
            //Перенаправление вывода ошибок и результатов на стандартные потоки
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

    //Проверка занятости порта
    public static boolean isPortInUse(int port) {
        try (Socket _ = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
