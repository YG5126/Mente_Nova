package mente.nova.mente_nova.minio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.minio.*;

import jakarta.annotation.PostConstruct;

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

    //Остановка сервера и вызод программы
    public void exit() {
        MinioServer.stopServer();
        System.exit(0);
    }

/*
    public void createFolderStructure(String bucketName) throws Exception {
        String[] folders = {
                "docs/2023/reports/",
                "logs/system/",
                "temp/"
        };

        for (String folder : folders) {
            String objectName = folder + "sample.txt";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream("".getBytes()), 0, -1)
                            .contentType("text/plain")
                            .build());
            System.out.println("File created: " + objectName);
        }
    }

    public void listTest(String bucketName) throws Exception {
        Set<String> folders = new TreeSet<>();
        Set<String> files = new TreeSet<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build());

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();

            if (objectName.endsWith("/")) {

                folders.add(objectName);
            } else {
                files.add(objectName);
                String parent = getParentFolder(objectName);
                while (parent != null) {
                    folders.add(parent);
                    parent = getParentFolder(parent);
                }
            }
        }

        System.out.println("Папки:");
        folders.forEach(folder -> System.out.println("[Папка] " + folder));

        System.out.println("\nФайлы:");
        files.forEach(file -> System.out.println("[Файл] " + file));
    }

    private String getParentFolder(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        if (lastSlash == -1)
            return null;
        return objectName.substring(0, lastSlash);
    }

    public ArrayList<String> list() {
        try {
            List<Bucket> bucketList = minioClient.listBuckets();
            ArrayList<String> buckets = new ArrayList<>();
            for (Bucket bucket : bucketList) {
                buckets.add(bucket.name());
            }
            return buckets;
        } catch (Exception e) {
            System.err.println("Ошибка при получении списка бакетов: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public ArrayList<String> list(String bucketName) {
        return list(bucketName, "");
    }

    public ArrayList<String> list(String bucketName, String path) {
        if (path.contains(".")) {
            System.out.println("Ошибка: Префиксный путь не может вести к файлу");
            return null;
        }
        try {

            if (!path.endsWith("/") && path.length() != 0) {
                path += "/";
            }

            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(path)
                    .delimiter("/")
                    .recursive(false)
                    .build();

            Iterable<Result<Item>> objects = minioClient.listObjects(args);
            ArrayList<String> items = new ArrayList<>();

            for (Result<Item> itemResult : objects) {

                String way = itemResult.get().objectName();

                if (path.equals(way.substring(0, path.length()))) {
                    way = way.replaceFirst(path, "");
                    if (way.contains("/")) {
                        items.add("folder$" + way.substring(0, way.indexOf('/')));
                    } else {
                        items.add("file$" + way);
                    }
                } else {
                    System.out.println("Ошибка: неверный путь");
                    return null;
                }

            }

            return items;

        } catch (Exception e) {
            System.err.println("Ошибка при получении списка объектов: " + e.getMessage());
        }
        return null;
    }

    public LinkedHashMap<String, LinkedHashMap> list(String bucketName, String path, int depth, LinkedHashMap items) {
        if (path.contains(".")) {
            System.out.println("Ошибка: Префиксный путь не может вести к файлу");
            return null;
        }
        try {

            if (!path.endsWith("/") && path.length() != 0) {
                path += "/";
            }

            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(path)
                    .delimiter("/")
                    .recursive(false)
                    .build();

            Iterable<Result<Item>> objects = minioClient.listObjects(args);

            for (Result<Item> itemResult : objects) {

                String way = itemResult.get().objectName();

                if (path.equals(way.substring(0, path.length()))) {
                    way = way.replaceFirst(path, "");
                    if (way.contains("/")) {
                        String folder = way.substring(0, way.indexOf('/'));
                        System.out.println("  ".repeat(depth) + "[Папка] " + folder);
                        //list(bucketName, path + folder, depth + 1);
                    } else {
                        System.out.println("  ".repeat(depth) + "[Файл] " + way);
                    }
                } else {
                    System.out.println("Ошибка: неверный путь");
                    return null;
                }

            }

        } catch (Exception e) {
            System.err.println("Ошибка при получении списка объектов: " + e.getMessage());
        }
        return null;
    }
*/

}
