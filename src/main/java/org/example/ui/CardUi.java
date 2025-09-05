package org.example.ui;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class CardUi {
    private CardUi() {}

    public static final String CARD_TYPE_DEBITO  = "Debito";
    public static final String CARD_TYPE_CREDITO = "Credito";
    public static final String DIGITS_ONLY_REGEX = "\\D";

    /** Inizializza la combo del tipo carta (Debito/Credito). */
    public static void setupTypeCombo(ComboBox<String> combo) {
        if (combo == null) return;
        combo.getItems().setAll(CARD_TYPE_DEBITO, CARD_TYPE_CREDITO);
        combo.setValue(CARD_TYPE_DEBITO);
    }

    /** Maschera il PAN mostrando solo le ultime 4 cifre. */
    public static String maskPan(String pan) {
        if (pan == null) return "";
        String digits = pan.replaceAll(DIGITS_ONLY_REGEX, "");
        if (digits.length() <= 4) return digits;
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }

    /** CVV valido = 3 cifre. */
    public static boolean isValidCvv(String cvv) {
        return cvv == null || !cvv.matches("\\d{3}");
    }

    /** Collega larghezze colonna alla larghezza della tabella (ratio 0..1). */
    public static void bindWidth(TableView<?> table, TableColumn<?, ?> col, double ratio) {
        if (table == null || col == null) return;
        col.prefWidthProperty().bind(table.widthProperty().multiply(ratio));
    }

    /** Disabilita 'Conferma' se lista vuota o nessuna riga selezionata. */
    public static void bindConfirmEnablement(ObservableList<?> items, TableView<?> table, Button confirmBtn) {
        if (confirmBtn == null || table == null || items == null) return;
        confirmBtn.setDisable(items.isEmpty() || table.getSelectionModel().getSelectedItem() == null);

        items.addListener((ListChangeListener<Object>) c ->
            confirmBtn.setDisable(items.isEmpty() || table.getSelectionModel().getSelectedItem() == null)
        );

        table.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) ->
            confirmBtn.setDisable(newV == null || items.isEmpty())
        );
    }
}
