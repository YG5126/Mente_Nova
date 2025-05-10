package mente.nova.mente_nova.service;

import mente.nova.mente_nova.config.ConfigManager;

import org.odftoolkit.odfdom.doc.OdfDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
@Component
public class odtApplication {

    @Autowired
    private MinioApplication minioClient;


    /**
     * Объединяет два ODT-файла (добавляет второй в конец первого) и сохраняет результат в MinIO.
     * @param serverFilePath Путь к первому ODT-файлу на сервере
     * @param serverFilePath1 Путь ко второму ODT-файлу на сервере
     */
    public void joinODT(String serverFilePath, String serverFilePath1) {
        File file1 = null;
        File file2 = null;
        File mergedFile = null;
        OdfDocument doc1 = null;
        OdfDocument doc2 = null;

        try {
            // Получаем файлы из MinIO
             String fullPath1 = ConfigManager.getValue("semester") + " семестр/" + serverFilePath;
             String fullPath2 = ConfigManager.getValue("semester") + " семестр/" + serverFilePath1;

             file1 = minioClient.returnFile(fullPath1);
             file2 = minioClient.returnFile(fullPath2);

            if (file1 == null || file2 == null) {
                Logger.error("Не удалось получить один или оба ODT файла для объединения.");
                return;
            }

            // Создаем временный файл для результата
            mergedFile = File.createTempFile("merged_odt_", ".odt");

            // Загружаем документы с использованием ODFDOM API
            doc1 = OdfDocument.loadDocument(file1);
            doc2 = OdfDocument.loadDocument(file2);

            // Проверяем, что оба документа текстовые
            if (!OdfDocument.OdfMediaType.TEXT.getMediaTypeString().equals(doc1.getMediaTypeString()) ||
                !OdfDocument.OdfMediaType.TEXT.getMediaTypeString().equals(doc2.getMediaTypeString())) {
                Logger.error("Один или оба файла для объединения не являются текстовыми документами ODT.");
                return;
            }

            // Получаем корневой элемент текста первого документа
            Node doc1Body = doc1.getContentRoot();

            // Получаем корневой элемент текста второго документа
            Node doc2Body = doc2.getContentRoot();
            NodeList contentToAppend = doc2Body.getChildNodes();

            // Добавляем разрыв страницы (опционально)
            // TextPElement pageBreak = doc1.getContentRoot().newTextPElement();
            // pageBreak.addStyleName("PageBreak"); // Имя стиля может отличаться
            // doc1Body.appendChild(pageBreak);

            // Копируем все дочерние узлы из тела второго документа в тело первого
            for (int i = 0; i < contentToAppend.getLength(); i++) {
                Node importedNode = doc1.getContentDom().importNode(contentToAppend.item(i), true);
                doc1Body.appendChild(importedNode);
            }

            // Сохраняем измененный первый документ
            doc1.save(mergedFile);

            // Загружаем объединенный файл обратно в MinIO
             String mergedFileName = "merged_" + serverFilePath; // Имя для сохранения в MinIO
             String mergedFileMinioPath = ConfigManager.getValue("semester") + " семестр/" + mergedFileName;

             minioClient.loadingFile(mergedFileMinioPath, mergedFile.getAbsolutePath());
            Logger.info("ODT файлы успешно объединены и сохранены как {}", mergedFileMinioPath);

        } catch (Exception e) {
            Logger.error("Ошибка при объединении ODT-файлов: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Закрываем документы ODFDOM
             if (doc1 != null) try { doc1.close(); } catch (Exception ignored) {}
             if (doc2 != null) try { doc2.close(); } catch (Exception ignored) {}

            // Удаляем все временные файлы
            if (file1 != null && file1.exists()) {
                if (!file1.delete()) Logger.warn("Не удалось удалить временный файл: {}", file1.getAbsolutePath());
            }
            if (file2 != null && file2.exists()) {
                 if (!file2.delete()) Logger.warn("Не удалось удалить временный файл: {}", file2.getAbsolutePath());
            }
             if (mergedFile != null && mergedFile.exists()) {
                 if (!mergedFile.delete()) Logger.warn("Не удалось удалить временный объединенный файл: {}", mergedFile.getAbsolutePath());
             }
        }
    }
}
