package mente.nova.mente_nova.minio;

import io.minio.*;
import io.minio.messages.Item;
import java.util.*;

public class MinioList {
    
    private final MinioClient minioClient;
    
    //Построение связи с клиентом MinIO
    public MinioList(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // Класс для представления узла дерева
    public static class Node {
        private String name;
        private boolean isDirectory;
        private Map<String, Node> children = new TreeMap<>();
        private boolean isBucket = false;
        
        public Node(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }
        
        // Геттеры
        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public Map<String, Node> getChildren() { return children; }
        public void setIsBucket() { this.isBucket = true; }
        public boolean isBucket() { return isBucket; }
        
        @Override
        //Перезапись стандартного метода для вывода дерева
        public String toString() {
            return toString(-1);
        }
        
        //Рекурсивный вывод дерева
        private String toString(int depth) {

            String output = "";
            if (!isBucket) {
                output = "  ".repeat(depth) + (isDirectory ? "[Папка] " : "[Файл] ") + name + "\n";
            }
            
            for (Node child : children.values()) {
                output += child.toString(depth + 1);
            }
            return output;
        }
    }

    //Построение древовидной структуры бакета
    public Node buildBucketTree(String bucketName) throws Exception {
        //Создание начального узла бакета
        Node root = new Node(bucketName, true);
        root.setIsBucket();
        
        //Получение списка объектов бакета
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        );

        //Обход списка объектов бакета
        for (Result<Item> result : results) {
            //Получение имени объекта
            String objectName = result.get().objectName();
            
            //Пропуск пустых папок
            if (objectName.isEmpty()) continue;
            
            //Инициализация двунаправленного списка содержомого пути
            Deque<String> path = new ArrayDeque<>(Arrays.asList(objectName.split("/")));
            //Вызов рекурсивного обхода
            addToTree(root, path, objectName.endsWith("/"), true);
        }
        
        return root;
    }

    //Построение древовидной структуры бакета с указанием пути и функцией рекурсивного обхода
    public Node buildBucketTree(String bucketName, String path, boolean recursive) throws Exception {
        //Если путь пустой - название бакета не выводить
        boolean isExistPath = false;
        //Создание начального узла бакета
        Node root = new Node(bucketName, true);
        root.setIsBucket();
        
        //Получение списка объектов бакета
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        );

        //Обход списка объектов бакета
        for (Result<Item> result : results) {
            //Получение имени объекта
            String objectName = result.get().objectName();

            //Если путь длинее имени объекта - пропуск
            if (path.length() > objectName.length()) continue;
            //Если путь совпадает с именем объекта - установка флага и обрезка имени объекта
            if (path.equals(objectName.substring(0, path.length()))) {
                isExistPath = true;
                objectName = objectName.replaceFirst(path, "");
            } else {
                continue;
            }

            //Пропуск пустых папок
            if (objectName.isEmpty()) continue;
            
            //Инициализация двунаправленного списка содержомого пути
            Deque<String> dequePath = new ArrayDeque<>(Arrays.asList(objectName.split("/")));
            //Вызов обхода вложенных папок и файлов с функцией рекурсивного обхода
            addToTree(root, dequePath, objectName.endsWith("/"), recursive);
        }

        //Если не был не разу найден - вывод сообщения об отсутствии указанного пути
        if (!isExistPath) {
            System.out.print("Ошибка: указанный путь не найден");
        }

        return root;
    }

    //Обход содержимого папки с указанием пути и функцией рекурсивного обхода
    private void addToTree(Node node, Deque<String> path, boolean isDirectory, boolean recursive) {
        //Если путь пустой - выход
        if (path.isEmpty()) return;
        
        //Если после пермещения по пути, путь стал пустой - выход
        String current = path.removeFirst();
        if (current.isEmpty()) return;
        
        //Если это последний элемент и это файл
        if ((path.isEmpty() && !isDirectory) || (current.equals(".") && !current.contains("/"))) {
            node.getChildren().put(current, new Node(current, false));
            return;
        }
        
        //Обработка директорий и создание узлов в дочерних элементах
        Node child = node.getChildren().computeIfAbsent(
            current, 
            k -> new Node(k, true)
        );
        
        //Рекурсивный обход дочерних элементов
        if (recursive) {
            addToTree(child, path, isDirectory, recursive);
        }
    }

}