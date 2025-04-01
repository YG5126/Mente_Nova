package mente.nova.mente_nova.minio;

import org.springframework.context.annotation.Configuration;
import org.tinylog.Logger;

import jakarta.annotation.PreDestroy;

import java.net.Socket;
import java.io.IOException;

@Configuration
public class MinioServer {

    //Запуск сервера MinIO
    public static void startServer() {
        try {

            if (isPortInUse(9000)) {
                Logger.error("Ошибка: Порт 9000 уже занят");
                stopServer();
            }

            //Запуск сервера с поомщью ProcessBuilder и bat-скрипта запуска MinIO
            Logger.info("Запуск MinIO сервера...");
            ProcessBuilder processBuilderStart = new ProcessBuilder("src/main/resources/MinIO/nova.bat");
            //Перенаправление вывода ошибок и результатов на стандартные потоки
            processBuilderStart.redirectErrorStream(true);
            processBuilderStart.start();

            Logger.info("Ожидание запуска MinIO сервера...");
            while (!isPortInUse(9000)) {
                //Ожидание...
            }
            
            Logger.info("MinIO сервер запущен");

        } catch (IOException e) {
            Logger.error("Ошибка: " + e.getMessage());
        }
    }


    @PreDestroy
    //Остановка сервера
    public static void stopServer() {
        try {
            
            //Остановка сервера с помощью ProcessBuilder и завершения всех процессов по имени minio.exe
            Logger.info("Остановка MinIO сервера...");
            ProcessBuilder processBuilderStop = new ProcessBuilder("cmd.exe", "/C", "taskkill /F /IM minio.exe /T");
            //Перенаправление вывода ошибок и результатов на стандартные потоки
            processBuilderStop.redirectErrorStream(true);
            processBuilderStop.start();

            Logger.info("Ожидание остановки MinIO сервера...");
            while (isPortInUse(9000)) {
                //Ожидание...
            }
            
            Logger.info("MinIO сервер остановлен");
            Logger.tag("work").info("КОНЕЦ РАБОТЫ");

        } catch (IOException e) {
            Logger.error("Ошибка: " + e.getMessage());
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
