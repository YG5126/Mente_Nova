package mente.nova.mente_nova.minio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.minio.*;
import io.minio.errors.ErrorResponseException;


import jakarta.annotation.PostConstruct;
import mente.nova.mente_nova.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class MinioApplication {

    //Инициализация клиента MinIO
    @Autowired
    private MinioClient minioClient;
    
    /**
     * Проверяет подключение к MinIO при инициализации.
     */
    @PostConstruct
    public void init() {
        try {
            //Попытка получения списка бакетов
            minioClient.listBuckets();
        } catch (Exception e) {
            System.err.println("Ошибка подключения к MinIO: " + e.getMessage());
            System.exit(1);
        }
    }

    public File returnFile(String serverFilePath) {
        String bucketName = ConfigManager.getValue("bucket");
        File tempFile = null;
        try {
            // Получаем имя файла из пути
            String fileName;
            if (serverFilePath.indexOf('.') != -1) {
                fileName = serverFilePath.substring(serverFilePath.lastIndexOf("/") + 1);
            } else {
                System.out.println("Предупреждение: файл без расширения: " + serverFilePath);
                fileName = serverFilePath;
            }
            
            // Создаем файл в временной директории с оригинальным именем
            tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            
            // Получаем поток из MinIO и копируем его во временный файл
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(serverFilePath)
                        .build())) {
                Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.err.println("Ошибка при получении файла по пути " + serverFilePath + " из бакета " + bucketName + ": " + e.getMessage());
            }
            
            return tempFile;
            
        } catch (Exception e) {
            System.err.println("Ошибка при получении файла: " + e.getMessage());
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return null;
        }
    }

    public void createEmptyFile(String serverFilePath) {
        String bucketName = ConfigManager.getValue("bucket");
        try {

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilePath)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                    .build()
            );
            System.out.println("Пустой файл успешно создан: " + serverFilePath);
        }
        catch (Exception e) {
            System.err.println("Ошибка при создании файла: " + e.getMessage());
        }
    }
    
    /**
     * Подсчитывает количество файлов в указанной директории.
     * @param bucketName Имя бакета
     * @param path Путь к директории
     * @return Количество файлов или -1 в случае ошибки
     */
    public int countFiles(String path) {
        try {
            MinioList.Node root = list(path, true);
            return root.getChildren().size();
        }
        catch (Exception e) {
            System.err.println("Ошибка при подсчёте файлов: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Загружает файл в MinIO хранилище.
     * @param bucketName Имя бакета
     * @param serverFilePath Путь к файлу на сервере
     * @param localFilePath Путь к локальному файлу
     */
    public void loadingFile(String serverFilePath, String localFilePath) {
        String bucketName = ConfigManager.getValue("bucket");
        try {
            boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
            if (!isExist) {
                System.out.println("Бакет " + bucketName + " не существует");
            }
            else {
                try {
                    minioClient.statObject(StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serverFilePath)
                            .build());
                    System.out.println("Ошибка: Файл с таким именем " + serverFilePath + " уже существует в бакете.");
                    return;
                }
                catch (ErrorResponseException e) {
                    //Бу
                }
    
                if (new File(localFilePath).exists()) {
                    minioClient.uploadObject(
                        UploadObjectArgs
                        .builder()
                        .bucket(bucketName)
                        .object(serverFilePath)
                        .filename(localFilePath)
                        .build()
                    );
                    System.out.println("Файл " + serverFilePath + " загружен в бакет " + bucketName);
                }
                else {
                    System.out.println("Файл " + localFilePath + " не существует");
                }
            };
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Удаляет файл из MinIO хранилища.
     * @param serverFilePath Путь к файлу на сервере
     */
    public void deleteFile(String serverFilePath) {
        String bucketName = ConfigManager.getValue("bucket");
        try {
            // Проверяем, существует ли файл перед удалением (опционально)
            try {
            System.out.println("Проверка существования файла " + serverFilePath + " в бакете " + bucketName);
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilePath)
                    .build()
            );
        } catch (ErrorResponseException e) {
            System.out.println("Файл " + serverFilePath + " не существует в бакете " + bucketName);
            return;
        }
    
        // Удаляем файл
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilePath)
                .build()
        );
        System.out.println("Файл " + serverFilePath + " удален из бакета " + bucketName);
    } catch (Exception e) {
            System.err.println("Ошибка при удалении файла: " + e.getMessage());
        }
    }
    
    /**
     * Получает список всех файлов в бакете.
     * @param bucketName Имя бакета
     * @return Дерево файлов и директорий
     * @throws Exception в случае ошибки при получении списка
     */
    public MinioList.Node list() throws Exception {
        String bucketName = ConfigManager.getValue("bucket");
        //Проверка существования бакета
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            MinioList list = new MinioList(minioClient);
            MinioList.Node root = list.buildBucketTree();
            return root;
        } else {
            System.out.println("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    /**
     * Получает список файлов в указанной директории бакета.
     * @param bucketName Имя бакета
     * @param path Путь к директории
     * @param recursive Флаг рекурсивного обхода
     * @return Дерево файлов и директорий
     * @throws Exception в случае ошибки при получении списка
     */
    public MinioList.Node list(String path, boolean recursive) throws Exception {
        String bucketName = ConfigManager.getValue("bucket");
        //Проверка существования бакета
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            //Проверка на корректный синтаксис полученного пути
            if (!path.endsWith("/") && path.length() != 0) {
                path += "/";
            }
            MinioList list = new MinioList(minioClient);
            MinioList.Node root = list.buildBucketTree(path, recursive);
            return root;
        } else {
            System.out.println("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    /**
     * Создает новый бакет.
     * @param bucketName Имя бакета для создания
     * @throws Exception в случае ошибки при создании бакета
     */
    public void createBucket(String bucketName) throws Exception {

        //Проверка существования бакета
        boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());

        if (!isExist) {
            try {
                //Создание бакета
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
                System.out.println("Бакет " + bucketName + " создан");
            } catch (Exception e) {
                System.err.println("Ошибка при создании бакета: " + e.getMessage());
            }
        } else {
            System.out.println("Бакет " + bucketName + " уже существует");
        }
    }

    /**
     * Удаляет бакет.
     * @param bucketName Имя бакета для удаления
     * @throws Exception в случае ошибки при удалении бакета
     */
    public void deleteBucket(String bucketName) throws Exception {

        //Проверка существования бакета
        boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());

        if (isExist) {
            try {
                try {
                    //Удаление бакета
                    minioClient.removeBucket(
                            RemoveBucketArgs.builder()
                                    .bucket(bucketName)
                                    .build());
                    System.out.println("Бакет " + bucketName + " удалён");
                } catch (Exception e) {
                    System.out.println("Ошибка: Бакет " + bucketName + " не пустой");
                }
            } catch (Exception e) {
                System.err.println("Ошибка: Не удалось удалить бакет" + e.getMessage());
            }
        } else {
            System.out.println("Ошибка: Бакет " + bucketName + " не существует");
        }
    }

    /**
     * Останавливает сервер MinIO и завершает работу приложения.
     */
    public void exit() {
        MinioServer.stopServer();
        System.exit(0);
    }
}