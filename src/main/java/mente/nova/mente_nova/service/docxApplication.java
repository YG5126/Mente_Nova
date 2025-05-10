package mente.nova.mente_nova.service;

import mente.nova.mente_nova.config.ConfigManager;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


@Component
public class docxApplication {

    @Autowired
    private MinioApplication minioClient;

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
