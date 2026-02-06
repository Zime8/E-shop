package org.example.util;

public final class CardValidator {
    private CardValidator() {}
    public static boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3}");
    }
}
