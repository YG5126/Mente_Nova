package mente.nova.mente_nova.pdf;

import mente.nova.mente_nova.minio.MinioApplication;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;

@Component
public class pdfApplication {

    @Autowired
    private MinioApplication minioClient;

    public void readPDF(String bucketName, String serverFilePath) {
        try {
            System.out.println("Попытка чтения файла: " + serverFilePath + " из бакета: " + bucketName);
            
            File pdfFile = minioClient.returnFile(bucketName, serverFilePath);
            if (pdfFile == null) {
                System.err.println("Не удалось получить файл");
                return;
            }

            try {
                PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfFile));
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
                pdfFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при чтении PDF-файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void joinPDF(String bucketName, String serverFilePath, String serverFilePath2) {
        File file1 = null;
        File file2 = null;
        try {
            // Получаем файлы из MinIO
            file1 = minioClient.returnFile(bucketName, serverFilePath);
            file2 = minioClient.returnFile(bucketName, serverFilePath2);
            
            if (file1 == null || file2 == null) {
                System.err.println("Не удалось получить один или оба файла");
                return;
            }

            // Создаем временный файл для результата
            File mergedFile = File.createTempFile("merged", ".pdf");

            // Объединяем PDF файлы
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.addSource(file1);
            merger.addSource(file2);
            merger.setDestinationFileName(mergedFile.getAbsolutePath());
            merger.mergeDocuments(null);

            // Загружаем объединенный файл обратно в MinIO
            minioClient.loadingFile(bucketName, "merged_" + serverFilePath, mergedFile.getAbsolutePath());
            System.out.println("PDF файлы успешно объединены и сохранены как merged_" + serverFilePath);
            
        } catch (Exception e) {
            System.err.println("Ошибка при объединении PDF-файлов: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Удаляем все временные файлы
            if (file1 != null) file1.delete();
            if (file2 != null) file2.delete();
        }
    }

}
