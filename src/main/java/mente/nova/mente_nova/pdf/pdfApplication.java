package mente.nova.mente_nova.pdf;

import mente.nova.mente_nova.config.ConfigManager;
import mente.nova.mente_nova.minio.MinioApplication;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Component
public class pdfApplication {

    @Autowired
    private MinioApplication minioClient;

    /**
     * Класс для хранения информации о странице PDF
     */
    public static class PdfPageData {
        private BufferedImage image;
        private String text;
        
        public PdfPageData(BufferedImage image, String text) {
            this.image = image;
            this.text = text;
        }

        public BufferedImage getImage() {
            return image;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Рендерит PDF-файл в список страниц с изображениями и текстом
     * @param bucketName Имя бакета в MinIO
     * @param serverFilePath Путь к файлу на сервере
     * @return Список объектов с изображениями и текстом страниц PDF
     */
    public List<PdfPageData> renderPdfPages(String bucketName, String serverFilePath) {
        List<PdfPageData> pageData = new ArrayList<>();
        File pdfFile = null;
        
        try {
            serverFilePath = ConfigManager.getValue("semester") + " семестр/" + serverFilePath;
            
            pdfFile = minioClient.returnFile(bucketName, serverFilePath);
            if (pdfFile == null) {
                System.err.println("Не удалось получить файл");
                return pageData;
            }
            
            PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfFile));
            try {
                PDFRenderer renderer = new PDFRenderer(document);
                PDFTextStripper textStripper = new PDFTextStripper();
                
                // Рендерим каждую страницу с пониженным разрешением для экономии памяти
                for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                    // Снижаем DPI до 100 для экономии памяти
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, 100);
                    
                    // Получаем текст страницы
                    textStripper.setStartPage(pageIndex + 1);
                    textStripper.setEndPage(pageIndex + 1);
                    String text = textStripper.getText(document);
                    
                    // Добавляем страницу в результат
                    pageData.add(new PdfPageData(image, text));
                    
                    // Принудительно вызываем сборщик мусора для каждой третьей страницы
                    if (pageIndex % 3 == 0) {
                        System.gc();
                    }
                }
            } finally {
                document.close();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при рендеринге PDF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }
        }
        
        return pageData;
    }

    /**
     * Читает PDF-файл и возвращает список страниц.
     * @param bucketName Имя бакета в MinIO
     * @param serverFilePath Путь к файлу на сервере
     * @return Список строк, где каждая строка - содержимое одной страницы
     */
    public List<String> readPDF(String bucketName, String serverFilePath) {
        List<String> pages = new ArrayList<>();
        try {
            serverFilePath = ConfigManager.getValue("semester") + " семестр/" + serverFilePath;
            
            File pdfFile = minioClient.returnFile(bucketName, serverFilePath);
            if (pdfFile == null) {
                System.err.println("Не удалось получить файл");
                return pages;
            }

            try {
                PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdfFile));
                try {
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    
                    // Читаем каждую страницу отдельно
                    for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                        pdfStripper.setStartPage(pageNum);
                        pdfStripper.setEndPage(pageNum);
                        String pageText = pdfStripper.getText(document);
                        pages.add(pageText);
                    }
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
        }
        return pages;
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
