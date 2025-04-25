package mente.nova.mente_nova.minio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

import jakarta.annotation.PostConstruct;
import mente.nova.mente_nova.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;

@Component
public class MinioApplication {

    private static final String bucketName = ConfigManager.getValue("bucket");

    //Инициализация клиента MinIO
    @Autowired
    private MinioClient minioClient;
    
    private static final HashMap<String, List<Date>> fileChangeHistory = new HashMap<>();

    /**
     * Проверяет подключение к MinIO при инициализации.
     * Пытается получить список бакетов для проверки соединения.
     * В случае неудачи завершает приложение с кодом 1.
     */
    @PostConstruct
    public void init() {
        try {
            //Попытка получения списка бакетов
            minioClient.listBuckets();
            Logger.info("Подключение к MinIO успешно установлено");
        } catch (Exception e) {
            Logger.error("Ошибка подключения к MinIO: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Удаляет папку и все её содержимое из хранилища MinIO.
     * 
     * @param serverFilePath путь к папке на сервере
     */
    public void annihilateFolder(String serverFilePath) {
        String semester = ConfigManager.getValue("semester") + " семестр/";
        if (!serverFilePath.startsWith(semester)) {
            serverFilePath = semester + serverFilePath;
        }
        try {
            List<String> subjects = new MinioList(minioClient).getSubjects(serverFilePath, true);
            if (subjects != null) {
            for (String subject : subjects) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(subject)
                    .build());
            }
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilePath)
                    .build());
                Logger.info("Папка " + serverFilePath + " удалена со всеми вложенными файлами");
            } else {
                Logger.error("Ошибка: Папка " + serverFilePath + " не найдена");
            }
        } catch (Exception e) {
            Logger.error("Ошибка при удалении папки: " + e.getMessage());
        }
    }

    /**
     * Скачивает файл из MinIO и возвращает его как объект File.
     * Создает временный файл с таким же именем, как в хранилище.
     * 
     * @param serverFilePath путь к файлу на сервере
     * @return объект File, содержащий скачанный файл, или null в случае ошибки
     */
    public File returnFile(String serverFilePath) {
        File tempFile = null;
        try {
            // Получаем имя файла из пути
            String fileName;
            if (serverFilePath.indexOf('.') != -1) {
                fileName = serverFilePath.substring(serverFilePath.lastIndexOf("/") + 1);
            } else {
                Logger.warn("Предупреждение: файл без расширения: " + serverFilePath);
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
                Logger.error("Ошибка при получении файла по пути " + serverFilePath + " из бакета " + bucketName + ": " + e.getMessage());
            }
            
            return tempFile;
            
        } catch (Exception e) {
            Logger.error("Ошибка при получении файла: " + e.getMessage());
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return null;
        }
    }

    /**
     * Создает пустую папку в хранилище MinIO.
     * 
     * @param serverFilePath путь к создаваемой папке
     */
    public void createEmptyFolder(String serverFilePath) {
        try {

            if (!serverFilePath.endsWith("/")) {
                serverFilePath = serverFilePath + "/";
            }

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilePath)
                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                    .build()
            );
            Logger.info("Пустая папка успешно создана: " + serverFilePath);
        }
        catch (Exception e) {
            Logger.error("Ошибка при создании папки: " + e.getMessage());
        }
    }
    
    /**
     * Подсчитывает количество файлов в указанной директории.
     * 
     * @param path путь к директории
     * @return количество файлов или -1 в случае ошибки
     */
    public int countFiles(String path) {
        try {
            MinioList.Node root = list(path, true);
            return root.getChildren().size();
        }
        catch (Exception e) {
            Logger.error("Ошибка при подсчёте файлов: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Загружает файл в MinIO хранилище.
     * Проверяет существование бакета и файла с таким именем перед загрузкой.
     * 
     * @param serverFilePath путь к файлу на сервере
     * @param localFilePath путь к локальному файлу
     */
    public void loadingFile(String serverFilePath, String localFilePath) {
        try {
            boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
            if (!isExist) {
                Logger.error("Бакет " + bucketName + " не существует");
            }
            else {
                try {
                    minioClient.statObject(StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(serverFilePath)
                            .build());
                    Logger.error("Ошибка: Файл с таким именем " + serverFilePath + " уже существует в бакете.");
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
                    Logger.info("Файл " + serverFilePath + " загружен в бакет " + bucketName);
                }
                else {
                    Logger.error("Файл " + localFilePath + " не существует");
                }
            };
        } catch (Exception e) {
            Logger.error("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Удаляет файл из MinIO хранилища.
     * Проверяет существование файла перед удалением.
     * 
     * @param serverFilePath путь к файлу на сервере
     */
    public void deleteFile(String serverFilePath) {
        try {
            // Проверяем, существует ли файл перед удалением (опционально)
            try {
            Logger.info("Проверка существования файла " + serverFilePath + " в бакете " + bucketName);
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilePath)
                    .build()
            );
        } catch (ErrorResponseException e) {
            Logger.error("Файл " + serverFilePath + " не существует в бакете " + bucketName);
            return;
        }
    
        // Удаляем файл
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(serverFilePath)
                .build()
        );
        Logger.info("Файл " + serverFilePath + " удален из бакета " + bucketName);
    } catch (Exception e) {
            Logger.error("Ошибка при удалении файла: " + e.getMessage());
        }
    }

    /**
     * Получает список дочерних элементов для указанного пути.
     * Если isRecursive равно true, возвращает все элементы рекурсивно,
     * иначе возвращает только элементы первого уровня.
     * 
     * @param path Путь к директории
     * @param isRecursive Флаг рекурсивного поиска
     * @return Список уникальных имен дочерних элементов
     */
    public List<String> listChildren(String path, boolean isRecursive) {
        List<String> children = new ArrayList<>();

        for (String name : new MinioList(minioClient).getSubjects(path, isRecursive)) {
            name = name.replace(path, "");
            if (name == "") continue;
            List<String> splitNames = Arrays.asList(name.split("/"));
            if (isRecursive) {
                for (String splitName : splitNames) {
                    if (children.contains(splitName)) continue;
                    children.add(splitName);
                }
            } else {
                if (children.contains(splitNames.get(0))) continue;
                children.add(splitNames.get(0));
            }
        }

        return children;
    }

    /**
     * Получает список всех файлов в бакете.
     * 
     * @return корневой узел дерева файлов и директорий
     * @throws Exception в случае ошибки при получении списка
     */
    public MinioList.Node list() throws Exception {
        //Проверка существования бакета
        if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            MinioList list = new MinioList(minioClient);
            MinioList.Node root = list.buildBucketTree();
            return root;
        } else {
            Logger.error("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    /**
     * Получает список файлов в указанной директории бакета.
     * 
     * @param path путь к директории
     * @param recursive флаг рекурсивного обхода
     * @return корневой узел дерева файлов и директорий
     * @throws Exception в случае ошибки при получении списка
     */
    public MinioList.Node list(String path, boolean recursive) throws Exception {
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
            Logger.error("Бакет " + bucketName + " не существует");
            return null;
        }
    }

    public long sizeReturn(File file) {
        try {
            if ((file.exists()) && (file.isFile())) {
                return file.length();
            }
            else {
                Logger.error("Файл не найден");
            }
            return -1;
        } catch (SecurityException e) {
            Logger.error("Исключение безопасности: недостаточно прав для доступа к файлу.");
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            Logger.error("Произошла непредвиденная ошибка при обработке файла: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }    
    }

    /**
     * Рекурсивно загружает папку и все её содержимое в MinIO хранилище.
     * 
     * @param serverFolderPath путь к папке на сервере
     * @param localFolderPath путь к локальной папке
     */
    public void loadingFolder(String serverFolderPath, String localFolderPath) {
        try {
            File localFolder = new File(localFolderPath);
            if (!localFolder.exists() || !localFolder.isDirectory()) {
                Logger.error("Ошибка: Папка " + localFolderPath + " не существует или не является директорией");
                return;
            }

            // Создаем папку в MinIO
            createEmptyFolder(serverFolderPath);

            // Рекурсивно загружаем все файлы и подпапки
            File[] files = localFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String newServerPath = serverFolderPath + file.getName();
                    if (file.isDirectory()) {
                        newServerPath += "/";
                        loadingFolder(newServerPath, file.getAbsolutePath());
                    } else {
                        loadingFile(newServerPath, file.getAbsolutePath());
                    }
                }
            }
            Logger.info("Папка " + localFolderPath + " успешно загружена в " + serverFolderPath);
        } catch (Exception e) {
            Logger.error("Ошибка при загрузке папки: " + e.getMessage());
        }
    }

    /**
     * Возвращает информацию о последнем изменении файла в виде HashMap.
     * Ключи: "Год", "месяц", "день", "день недели", "час", "минуты", "секунда".
     * @param file Файл для проверки
     * @return HashMap<String, Integer> с компонентами даты/времени последнего изменения или пустой HashMap в случае ошибки.
     */
    public HashMap<String, Integer> lastChanges(File file) {
        HashMap<String, Integer> lastChangeInfo = new HashMap<>();
        try {
            if (file.exists() && file.isFile()) {
                long lastModifiedMillis = file.lastModified();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastModifiedMillis);

                lastChangeInfo.put("год", calendar.get(Calendar.YEAR));
                lastChangeInfo.put("месяц", calendar.get(Calendar.MONTH) + 1);
                lastChangeInfo.put("день", calendar.get(Calendar.DAY_OF_MONTH));
                // Дни недели: 1 (Воскресенье) - 7 (Суббота).
                lastChangeInfo.put("день недели", calendar.get(Calendar.DAY_OF_WEEK));
                lastChangeInfo.put("час", calendar.get(Calendar.HOUR_OF_DAY));
                lastChangeInfo.put("минута", calendar.get(Calendar.MINUTE));
                lastChangeInfo.put("секунда", calendar.get(Calendar.SECOND));

                Logger.info("Информация о последнем изменении файла '{}': год={}, месяц={}, день={}, день недели={}, час={}, минута={}, секунда={}",
                        file.getAbsolutePath(),
                        lastChangeInfo.get("год"),
                        lastChangeInfo.get("месяц"),
                        lastChangeInfo.get("день"),
                        lastChangeInfo.get("день недели"),
                        lastChangeInfo.get("час"),
                        lastChangeInfo.get("минута"),
                        lastChangeInfo.get("секунда"));

            } else {
                Logger.error("Файл не найден или не является файлом: {}", file.getAbsolutePath());
            }
        } catch (SecurityException e) {
            Logger.error("Исключение безопасности: недостаточно прав для доступа к файлу '{}'. {}", file.getAbsolutePath(), e.getMessage());
        } catch (Exception e) {
            Logger.error("Произошла непредвиденная ошибка при обработке файла '{}': {}", file.getAbsolutePath(), e.getMessage());
        }
        return lastChangeInfo;
    }

    /**
     * Перемещает файл из одного пути в другой внутри MinIO.
     * @param serverFilePathFrom Исходный путь файла в MinIO.
     * @param serverFilePathTo Целевой путь файла в MinIO.
     */
    public void fileMoving(String serverFilePathFrom, String serverFilePathTo) {
        // Проверяем, что пути не null и не совпадают
        if (serverFilePathFrom == null || serverFilePathTo == null || serverFilePathFrom.isEmpty() || serverFilePathTo.isEmpty()) {
            Logger.error("Ошибка: Исходный или целевой путь не указан.");
            return;
        }
        if (serverFilePathFrom.equals(serverFilePathTo)) {
            Logger.warn("Исходный и целевой пути совпадают. Перемещение не требуется: {}", serverFilePathFrom);
            return;
        }

        try {
            // 1. Проверяем, существует ли исходный объект
            try {
                minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(serverFilePathFrom)
                        .build()
                );
                Logger.info("Исходный файл {} найден в бакете {}.", serverFilePathFrom, bucketName);
            } catch (ErrorResponseException e) {
                // Проверяем, является ли ошибка "NoSuchKey" (объект не найден)
                if (e.errorResponse() != null && "NoSuchKey".equals(e.errorResponse().code())) {
                    Logger.error("Ошибка перемещения: Исходный файл {} не найден в бакете {}.", serverFilePathFrom, bucketName);
                } else {
                    // Другая ошибка при проверке объекта
                    Logger.error("Ошибка при проверке существования исходного файла {}: {}", serverFilePathFrom, e.getMessage());
                }
                // Прерываем выполнение, если исходный файл не найден или произошла другая ошибка проверки
                return;
            }

            // 2. Копируем объект в новое место
            Logger.info("Начало копирования {} в {}", serverFilePathFrom, serverFilePathTo);
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .source(CopySource.builder()
                        .bucket(bucketName)
                        .object(serverFilePathFrom)
                        .build())
                    .bucket(bucketName)
                    .object(serverFilePathTo)
                    .build()
            );
            Logger.info("Файл {} успешно скопирован в {}.", serverFilePathFrom, serverFilePathTo);

            // 3. Удаляем исходный объект после успешного копирования
            Logger.info("Начало удаления исходного файла {}", serverFilePathFrom);
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(serverFilePathFrom)
                    .build()
            );
            Logger.info("Исходный файл {} успешно удален.", serverFilePathFrom);

            // Выводим сообщение об успехе в консоль
            System.out.println("Файл \"" + serverFilePathFrom + "\" успешно перемещён в \"" + serverFilePathTo + "\"");

        } catch (ErrorResponseException e) {
            // Обработка специфичных ошибок MinIO (например, проблемы с доступом)
            Logger.error("Ошибка ответа MinIO при перемещении файла {} в {}: Код={}, Сообщение={}", serverFilePathFrom, serverFilePathTo, e.errorResponse() != null ? e.errorResponse().code() : "N/A", e.getMessage());
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            Logger.error("Ошибка чтения данных при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (InternalException e) {
            Logger.error("Внутренняя ошибка MinIO/XML при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            Logger.error("Неверный ответ от сервера MinIO при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            // Общая ошибка ввода-вывода (например, сетевые проблемы)
            Logger.error("Ошибка ввода-вывода при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (ServerException e) {
            Logger.error("Ошибка на стороне сервера MinIO при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (XmlParserException e) {
            // Ошибка парсинга XML ответа от MinIO
            Logger.error("Ошибка парсинга XML ответа MinIO при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // Исключение из java.security
            Logger.error("Ошибка ключа доступа (java.security) при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // Исключение из java.security
            Logger.error("Ошибка алгоритма (java.security) при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Обработка любых других непредвиденных исключений
            Logger.error("Непредвиденная ошибка при перемещении файла {} в {}: {}", serverFilePathFrom, serverFilePathTo, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Создает новый бакет.
     * Проверяет, что бакет с таким именем не существует.
     * 
     * @param createBucketName имя бакета для создания
     * @throws Exception в случае ошибки при создании бакета
     */
    public void createBucket(String createBucketName) throws Exception {

        //Проверка существования бакета
        boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(createBucketName)
                        .build());

        if (!isExist) {
            try {
                //Создание бакета
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(createBucketName)
                                .build());
                Logger.info("Бакет " + createBucketName + " создан");
            } catch (Exception e) {
                Logger.error("Ошибка при создании бакета: " + e.getMessage());
            }
        } else {
            Logger.error("Бакет " + createBucketName + " уже существует");
        }
    }

    /**
     * Удаляет бакет.
     * Проверяет, что бакет существует и пустой перед удалением.
     * 
     * @param deleteBucketName имя бакета для удаления
     * @throws Exception в случае ошибки при удалении бакета
     */
    public void deleteBucket(String deleteBucketName) throws Exception {

        //Проверка существования бакета
        boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(deleteBucketName)
                        .build());

        if (isExist) {
            try {
                try {
                    //Удаление бакета
                    minioClient.removeBucket(
                            RemoveBucketArgs.builder()
                                    .bucket(deleteBucketName)
                                    .build());
                    Logger.info("Бакет " + deleteBucketName + " удалён");
                } catch (Exception e) {
                    Logger.error("Ошибка: Бакет " + deleteBucketName + " не пустой");
                }
            } catch (Exception e) {
                Logger.error("Ошибка: Не удалось удалить бакет" + e.getMessage());
            }
        } else {
            Logger.error("Ошибка: Бакет " + deleteBucketName + " не существует");
        }
    }

    /**
     * Останавливает сервер MinIO и завершает работу приложения.
     * Вызывает метод остановки сервера и завершает JVM с кодом 0.
     */
    public void exit() {
        MinioServer.stopServer();
        System.exit(0);
    }

}