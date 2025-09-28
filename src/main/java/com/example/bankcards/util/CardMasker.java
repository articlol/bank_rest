package com.example.bankcards.util;

/**
 * Утилита для маскирования номера карты с показом только последних 4 цифр.
 */
public final class CardMasker {
    private CardMasker() {}

    /**
     * Возвращает маску в формате: "**** **** **** 1234".
     */
    public static String mask(String fullNumber) {
        if (fullNumber == null || fullNumber.length() < 4) {
            return "****";
        }
        String last4 = fullNumber.substring(fullNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
