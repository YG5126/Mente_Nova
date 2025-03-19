package mente.nova.mente_nova.minio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.minio.*;
import io.minio.errors.ErrorResponseException;


import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class MinioApplication {

    //Инициализация клиента MinIO
    @Autowired
    private MinioClient minioClient;

    //Проверка инициализации клиента MinIO
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


    public void createEmptyFile(String bucketName, String serverFilePath) {
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

    public int countFiles(String bucketName, String path) {
        try {
            MinioList.Node root = list(bucketName, path, true);
            return root.getChildren().size();
        }
        catch (Exception e) {
            System.err.println("Ошибка при подсчёте файлов: " + e.getMessage());
            return -1;
        }
    }

    public void loadingFile(String bucketName, String serverFilePath, String localFilePath) {
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

    public void deleteFile(String bucketName, String serverFilePath) {
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
    
    //Рекурсивный обход бакета
    public MinioList.Node list(String bucketName) throws Exception {
        //Проверка существования бакета
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            MinioList list = new MinioList(minioClient);
            MinioList.Node root = list.buildBucketTree(bucketName);
            return root;
        } else {
            System.out.println("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    //Обход бакета с указанием пути и функцией рекурсивного обхода
    public MinioList.Node list(String bucketName, String path, boolean recursive) throws Exception {
        //Проверка существования бакета
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            //Проверка на корректный синтаксис полученного пути
            if (!path.endsWith("/") && path.length() != 0) {
                path += "/";
            }
            MinioList list = new MinioList(minioClient);
            MinioList.Node root = list.buildBucketTree(bucketName, path, recursive);
            return root;
        } else {
            System.out.println("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    //Создание бакета
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

    //Удаление бакета
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

    public void readPDF(String bucketName, String serverFilePath) {
        try {
            // Проверка существования файла
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(serverFilePath).build());
    
            // Загрузка файла в поток
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serverFilePath)
                            .build())) {
    
                // Загрузка во временный файл и чтение
                File tempFile = File.createTempFile("temp", ".pdf");
                try {
                    Files.copy(stream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(tempFile));
                    try {
                        PDFTextStripper pdfStripper = new PDFTextStripper();
                        String text = pdfStripper.getText(document);
                        System.out.println("Содержимое PDF-файла:\n" + text);
                    } finally {
                        if (document != null) {
                            document.close();
                        }
                    }
                } finally {
                    tempFile.delete();
                }
            }
        } catch (ErrorResponseException e) {
            System.err.println("Ошибка: Файл " + serverFilePath + " не найден в бакете " + bucketName);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении PDF-файла: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Общая ошибка: " + e.getMessage());
        }
    }

    //Остановка сервера и вызод программы
    public void exit() {
        MinioServer.stopServer();
        System.exit(0);
    }
}
