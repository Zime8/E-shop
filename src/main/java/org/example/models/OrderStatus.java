package org.example.models;

public enum OrderStatus {
    IN_ELABORAZIONE, SPEDITO, CONSEGNATO, ANNULLATO;

    public static OrderStatus fromDb(String s) {
        if (s == null) return IN_ELABORAZIONE;
        return switch (s.trim().toLowerCase()) {
            case "spedito"         -> SPEDITO;
            case "consegnato"      -> CONSEGNATO;
            case "annullato"       -> ANNULLATO;
            default -> IN_ELABORAZIONE;
        };
    }

    public String toDb() {
        return switch (this) {
            case IN_ELABORAZIONE -> "in elaborazione";
            case SPEDITO         -> "spedito";
            case CONSEGNATO      -> "consegnato";
            case ANNULLATO       -> "annullato";
        };
    }
}
