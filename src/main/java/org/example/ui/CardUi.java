package org.example.ui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.models.Card;

import java.util.Map;

public final class CardUi {
    private CardUi() {}

    public static final String CARD_TYPE_DEBITO  = "Debito";
    public static final String CARD_TYPE_CREDITO = "Credito";
    public static final String DIGITS_ONLY_REGEX = "\\D";

    /** Raggruppa riferimenti “di contesto” della tabella. */
    public record CardTableContext(
            TableView<Card> table,
            ObservableList<Card> items,
            Map<Integer, String> transientCvvs
    ) {}

    /** Raggruppa tutte le colonne della tabella carte. */
    public record CardColumns(
            TableColumn<Card, Number> colId,
            TableColumn<Card, String> colHolder,
            TableColumn<Card, String> colNumber,
            TableColumn<Card, String> colExpiry,
            TableColumn<Card, String> colType,
            TableColumn<Card, String> colCvv
    ) {}

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
        return cvv != null && cvv.matches("\\d{3}");
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
        items.addListener((InvalidationListener) c ->
                confirmBtn.setDisable(items.isEmpty() || table.getSelectionModel().getSelectedItem() == null)
        );
        table.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) ->
                confirmBtn.setDisable(newV == null || items.isEmpty())
        );
    }

    /**
     * Inizializzazione completa della tabella carte con 2 parametri (no più di 7).
     */
    public static void initCardTable(CardTableContext ctx, CardColumns cols) {
        if (ctx == null || ctx.table() == null) return;

        // Items + policy
        ctx.table().setItems(ctx.items());
        ctx.table().setEditable(true);
        ctx.table().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Colonne standard
        if (cols.colId()     != null) cols.colId().setCellValueFactory(c -> c.getValue().idProperty());
        if (cols.colHolder() != null) cols.colHolder().setCellValueFactory(c -> c.getValue().holderProperty());
        if (cols.colNumber() != null) cols.colNumber().setCellValueFactory(c -> new SimpleStringProperty(maskPan(c.getValue().getNumber())));
        if (cols.colExpiry() != null) cols.colExpiry().setCellValueFactory(c -> c.getValue().expiryProperty());
        if (cols.colType()   != null) cols.colType().setCellValueFactory(c -> c.getValue().typeProperty());

        // Colonna CVV
        if (cols.colCvv() != null) {
            cols.colCvv().setEditable(true);
            cols.colCvv().setCellValueFactory(c -> new SimpleStringProperty(""));
            cols.colCvv().setCellFactory(tc -> new CvvTableCell(ctx.table(), ctx.transientCvvs()));
        }

        // Larghezze
        bindWidth(ctx.table(), cols.colId(),     0.06);
        bindWidth(ctx.table(), cols.colHolder(), 0.34);
        bindWidth(ctx.table(), cols.colNumber(), 0.30);
        bindWidth(ctx.table(), cols.colExpiry(), 0.10);
        bindWidth(ctx.table(), cols.colType(),   0.10);
        bindWidth(ctx.table(), cols.colCvv(),    0.10);
    }
}
