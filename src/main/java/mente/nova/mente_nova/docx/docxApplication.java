package mente.nova.mente_nova.docx;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.MinioApplication;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class docxApplication {

    @Autowired
    private MinioApplication minioClient;

    /**
     * Класс для хранения информации о странице DOCX (пока только текст)
     */
    public static class DocxPageData {
        private String text;
        // В будущем можно добавить извлечение изображений, если необходимо

        public DocxPageData(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Читает DOCX-файл из MinIO и извлекает текст.
     * @param serverFilePath Путь к файлу на сервере
     * @return Список объектов DocxPageData (пока один объект с полным текстом)
     */
    public List<DocxPageData> readDOCX(String serverFilePath) {
        List<DocxPageData> pageData = new ArrayList<>();
        File docxFile = null;

        try {
            serverFilePath = ConfigManager.getValue("semester") + " семестр/" + serverFilePath;

            docxFile = minioClient.returnFile(serverFilePath);
            if (docxFile == null) {
                Logger.error("Не удалось получить файл DOCX: {}", serverFilePath);
                return pageData;
            }

            try (FileInputStream fis = new FileInputStream(docxFile);
                 XWPFDocument document = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

                String text = extractor.getText();
                // Для DOCX сложно разделить текст по страницам, как в PDF.
                // Пока возвращаем весь текст как одну "страницу".
                pageData.add(new DocxPageData(text));

            }
        } catch (Exception e) {
            Logger.error("Ошибка при чтении DOCX: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (docxFile != null && docxFile.exists()) {
                if (!docxFile.delete()) {
                     Logger.warn("Не удалось удалить временный файл DOCX: {}", docxFile.getAbsolutePath());
                }
            }
        }

        return pageData;
    }


    /**
     * Объединяет два DOCX-файла (добавляет второй в конец первого) и сохраняет результат в MinIO.
     * @param serverFilePath Путь к первому DOCX-файлу на сервере
     * @param serverFilePath1 Путь ко второму DOCX-файлу на сервере
     */
    public void joinDOCX(String serverFilePath, String serverFilePath1) {
        File file1 = null;
        File file2 = null;
        File mergedFile = null;

        try {
            // Получаем файлы из MinIO
             String fullPath1 = ConfigManager.getValue("semester") + " семестр/" + serverFilePath;
             String fullPath2 = ConfigManager.getValue("semester") + " семестр/" + serverFilePath1;

             file1 = minioClient.returnFile(fullPath1);
             file2 = minioClient.returnFile(fullPath2);

            if (file1 == null || file2 == null) {
                Logger.error("Не удалось получить один или оба DOCX файла для объединения.");
                return;
            }

            // Создаем временный файл для результата
            mergedFile = File.createTempFile("merged_docx_", ".docx");

            // Объединяем документы (простое добавление)
            try (FileInputStream fis1 = new FileInputStream(file1);
                 XWPFDocument doc1 = new XWPFDocument(fis1);
                 FileInputStream fis2 = new FileInputStream(file2);
                 XWPFDocument doc2 = new XWPFDocument(fis2);
                 FileOutputStream fos = new FileOutputStream(mergedFile)) {

                 // Добавляем разрыв страницы перед вставкой второго документа (опционально)
                 // doc1.createParagraph().setPageBreak(true);

                 // Копируем тело второго документа в первый
                 CTBody srcBody = doc2.getDocument().getBody();
                 doc1.getDocument().addNewBody().set(srcBody);

                 doc1.write(fos);
            }


            // Загружаем объединенный файл обратно в MinIO
             String mergedFileName = "merged_" + serverFilePath; // Имя для сохранения в MinIO
             String mergedFileMinioPath = ConfigManager.getValue("semester") + " семестр/" + mergedFileName;

             minioClient.loadingFile(mergedFileMinioPath, mergedFile.getAbsolutePath());
            Logger.info("DOCX файлы успешно объединены и сохранены как {}", mergedFileMinioPath);

        } catch (Exception e) {
            Logger.error("Ошибка при объединении DOCX-файлов: " + e.getMessage());
            e.printStackTrace();
        } finally {
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
