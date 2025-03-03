package mente.nova.mente_nova.minio;

import io.minio.*;
import io.minio.messages.Item;
import java.util.*;

public class MinioList {
    
    private final MinioClient minioClient;
    
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
        
        // Getters
        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public Map<String, Node> getChildren() { return children; }
        public void setIsBucket() { this.isBucket = true; }
        public boolean isBucket() { return isBucket; }
        
        @Override
        public String toString() {
            return toString(-1);
        }
        
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
     * Строит древовидную структуру бакета
     */
    public Node buildBucketTree(String bucketName) throws Exception {
        Node root = new Node(bucketName, true);
        root.setIsBucket();
        
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        );

        for (Result<Item> result : results) {
            String objectName = result.get().objectName();
            
            // Пропускаем пустые "папки"
            if (objectName.isEmpty()) continue;
            
            Deque<String> path = new ArrayDeque<>(Arrays.asList(objectName.split("/")));
            addToTree(root, path, objectName.endsWith("/"), true);
        }
        
        return root;
    }

        /**
     * Строит древовидную структуру бакета
     */
    public Node buildBucketTree(String bucketName, String path, boolean recursive) throws Exception {
        boolean isExistPath = false;
        Node root = new Node(bucketName, true);
        root.setIsBucket();
        
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .build()
        );

        for (Result<Item> result : results) {
            String objectName = result.get().objectName();
            if (path.length() > objectName.length()) continue;
            if (path.equals(objectName.substring(0, path.length()))) {
                isExistPath = true;
                objectName = objectName.replaceFirst(path, "");
            } else {
                continue;
            }

            if (objectName.isEmpty()) continue;
            
            Deque<String> dequePath = new ArrayDeque<>(Arrays.asList(objectName.split("/")));
            addToTree(root, dequePath, objectName.endsWith("/"), recursive);
        }

        if (!isExistPath) {
            System.out.print("Ошибка: указанный путь не найден");
        }

        return root;
    }


    private void addToTree(Node node, Deque<String> path, boolean isDirectory, boolean recursive) {
        if (path.isEmpty()) return;
        
        String current = path.removeFirst();
        if (current.isEmpty()) return;
        
        // Если это последний элемент и это файл
        if ((path.isEmpty() && !isDirectory) || (current.equals(".") && !current.contains("/"))) {
            node.getChildren().put(current, new Node(current, false));
            return;
        }
        
        // Обработка директорий
        Node child = node.getChildren().computeIfAbsent(
            current, 
            k -> new Node(k, true)
        );
        
        if (recursive) {
            addToTree(child, path, isDirectory, recursive);
        }
    }

}