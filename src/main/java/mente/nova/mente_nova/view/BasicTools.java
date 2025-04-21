package mente.nova.mente_nova.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicTools {

    /**
     * Проверяет, содержит ли входная строка только символы из разрешенного списка.
     * 
     * @param input строка для проверки
     * @param allowedChars список разрешенных символов
     * @return true если строка содержит только разрешенные символы, иначе false
     */
    public static boolean containsOnlyAllowedChars(String input, List<Character> allowedChars) {
        Set<Character> allowedSet = new HashSet<>();
        for (char c : allowedChars) {
            allowedSet.add(c);
        }
    
        for (char c : input.toCharArray()) {
            if (!allowedSet.contains(c)) {
                return false;
            }
        }
        return true;
    }

}
