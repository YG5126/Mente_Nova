package mente.nova.mente_nova.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.tinylog.Logger;

import java.io.File;

/**
 * Менеджер конфигурации приложения.
 * Управляет сохранением и загрузкой настроек приложения в файл properties.
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "src/main/resources/config.properties";
    private static Properties properties;
    
    /**
     * Инициализация свойств Properties и загрузка существующих настроек.
     */
    static {
        properties = new Properties();
        loadProperties();
    }
    
    /**
     * Загружает настройки из файла конфигурации.
     */
    private static void loadProperties() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                properties.load(fis);
                fis.close();
            }
        } catch (Exception e) {
            Logger.error("Ошибка при загрузке конфигурации: " + e.getMessage());
        }
    }
    
    /**
     * Сохраняет текущие настройки в файл конфигурации.
     */
    private static void saveProperties() {
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            properties.store(fos, "Mente-Nova Settings");
            fos.close();
        } catch (Exception e) {
            Logger.error("Ошибка при сохранении конфигурации: " + e.getMessage());
        }
    }
    
    /**
     * Получает значение по указанному ключу.
     * @param key Ключ для поиска значения
     * @return Значение, соответствующее ключу, или null если не найдено
     */
    public static String getValue(String key) {
        return properties.getProperty(key);
    }

    /**
     * Получает значение по указанному ключу с значением по умолчанию.
     * @param key Ключ для поиска значения
     * @param defaultValue Значение по умолчанию, если ключ не найден
     * @return Значение, соответствующее ключу, или значение по умолчанию
     */
    public static String getValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Устанавливает значение для указанного ключа.
     * @param key Ключ для установки значения
     * @param value Значение для установки
     */
    public static void setValue(String key, String value) {
        properties.setProperty(key, value);
        saveProperties();
        Logger.info("Значение для ключа {} установлено: {}", key, value);
    }

    /**
     * Удаляет значение по указанному ключу.
     * @param key Ключ для удаления
     */
    public static void deleteValue(String key) {
        properties.remove(key);
        saveProperties();
        Logger.info("Значение для ключа {} удалено", key);
    }
}
