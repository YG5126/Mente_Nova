package mente.nova.mente_nova.minio;

import io.minio.*;
import io.minio.messages.Item;
import java.util.*;
import mente.nova.mente_nova.config.ConfigManager;

/**
 * Класс для работы со списком файлов в MinIO хранилище.
 * Предоставляет функциональность для построения и обхода древовидной структуры файлов.
 */
public class MinioList {
    
    private final MinioClient minioClient;
    
    /**
     * Конструктор класса.
     * @param minioClient Клиент MinIO для работы с хранилищем
     */
    public MinioList(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Внутренний класс для представления узла в дереве файлов.
     */
    public static class Node {
        private String name;
        private boolean isDirectory;
        private Map<String, Node> children = new TreeMap<>();
        private boolean isBucket = false;
        
        /**
         * Конструктор узла.
         * @param name Имя узла
         * @param isDirectory Флаг, указывающий является ли узел директорией
         */
        public Node(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }
        
        /**
         * Получает имя узла.
         * @return Имя узла
         */
        public String getName() { return name; }
        
        /**
         * Проверяет, является ли узел директорией.
         * @return true если узел является директорией
         */
        public boolean isDirectory() { return isDirectory; }
        
        /**
         * Получает дочерние узлы.
         * @return Map дочерних узлов
         */
        public Map<String, Node> getChildren() { return children; }
        
        /**
         * Устанавливает флаг бакета.
         */
        public void setIsBucket() { this.isBucket = true; }
        
        /**
         * Проверяет, является ли узел бакетом.
         * @return true если узел является бакетом
         */
        public boolean isBucket() { return isBucket; }
        
        @Override
        //Перезапись стандартного метода для вывода дерева
        public String toString() {
            return toString(-1);
        }
        
        /**
         * Рекурсивно формирует строковое представление дерева.
         * @param depth Текущая глубина в дереве
         * @return Строковое представление узла и его потомков
         */
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

    /**
     * Строит древовидную структуру бакета.
     * @param bucketName Имя бакета
     * @return Корневой узел дерева
     * @throws Exception в случае ошибки при построении дерева
     */
    public Node buildBucketTree() throws Exception {
        String bucketName = ConfigManager.getValue("bucket");
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

    /**
     * Строит древовидную структуру бакета с указанием пути.
     * @param bucketName Имя бакета
     * @param path Путь к директории
     * @param recursive Флаг рекурсивного обхода
     * @return Корневой узел дерева
     * @throws Exception в случае ошибки при построении дерева
     */
    public Node buildBucketTree(String path, boolean recursive) throws Exception {
        String bucketName = ConfigManager.getValue("bucket");
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

    /**
     * Добавляет узел в дерево.
     * @param node Текущий узел
     * @param path Путь к добавляемому узлу
     * @param isDirectory Флаг, указывающий является ли узел директорией
     * @param recursive Флаг рекурсивного обхода
     */
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