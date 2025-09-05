package org.example.controllers.support;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.models.Card;
import org.example.services.CardsService;
import org.example.ui.CardUi;
import org.example.ui.CvvTableCell;
import org.example.ui.FxUi;

import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CardScreenHelper {

    private final TableView<Card> table;
    private final TableColumn<Card, Number> colId;
    private final TableColumn<Card, String> colHolder;
    private final TableColumn<Card, String> colNumber;
    private final TableColumn<Card, String> colExpiry;
    private final TableColumn<Card, String> colType;
    private final TableColumn<Card, String> colCvv;

    private final ComboBox<String> typeCombo;
    private final ObservableList<Card> items;
    private final Map<Integer, String> transientCvvs;

    private final Button confirmBtn;
    private final ProgressIndicator progress;
    private final Logger logger;
    private final Consumer<String> onInfo;
    private final Consumer<String> onError;

    public CardScreenHelper(
            TableView<Card> table,
            TableColumn<Card, Number> colId,
            TableColumn<Card, String> colHolder,
            TableColumn<Card, String> colNumber,
            TableColumn<Card, String> colExpiry,
            TableColumn<Card, String> colType,
            TableColumn<Card, String> colCvv,
            ComboBox<String> typeCombo,
            ObservableList<Card> items,
            Map<Integer, String> transientCvvs,
            Button confirmBtn,
            ProgressIndicator progress,
            Logger logger,
            Consumer<String> onInfo,
            Consumer<String> onError
    ) {
        this.table = table;
        this.colId = colId;
        this.colHolder = colHolder;
        this.colNumber = colNumber;
        this.colExpiry = colExpiry;
        this.colType = colType;
        this.colCvv = colCvv;
        this.typeCombo = typeCombo;
        this.items = items;
        this.transientCvvs = transientCvvs;
        this.confirmBtn = confirmBtn;
        this.progress = progress;
        this.logger = (logger != null) ? logger : Logger.getLogger(CardScreenHelper.class.getName());
        this.onInfo = (onInfo != null) ? onInfo : s -> {};
        this.onError = (onError != null) ? onError : s -> {};
    }

    /** Inizializza combo, tabella (colonne + cvv), binding larghezze e abilitazione bottone Confirm. */
    public void initUi() {
        CardUi.setupTypeCombo(typeCombo);

        if (table != null) {
            table.setItems(items);
            table.setEditable(true);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }

        if (colId != null)     colId.setCellValueFactory(c -> c.getValue().idProperty());
        if (colHolder != null) colHolder.setCellValueFactory(c -> c.getValue().holderProperty());
        if (colNumber != null) colNumber.setCellValueFactory(c ->
                new SimpleStringProperty(CardUi.maskPan(c.getValue().getNumber())));
        if (colExpiry != null) colExpiry.setCellValueFactory(c -> c.getValue().expiryProperty());
        if (colType != null)   colType.setCellValueFactory(c -> c.getValue().typeProperty());

        if (colCvv != null) {
            colCvv.setEditable(true);
            colCvv.setCellValueFactory(c -> new SimpleStringProperty(""));
            colCvv.setCellFactory(tc -> new CvvTableCell(table, transientCvvs));
        }

        CardUi.bindWidth(table, colId,     0.06);
        CardUi.bindWidth(table, colHolder, 0.34);
        CardUi.bindWidth(table, colNumber, 0.30);
        CardUi.bindWidth(table, colExpiry, 0.10);
        CardUi.bindWidth(table, colType,   0.10);
        CardUi.bindWidth(table, colCvv,    0.10);

        CardUi.bindConfirmEnablement(items, table, confirmBtn);

        if (progress != null) progress.setVisible(false);
    }

    /** Carica le carte salvate dell'utente nell'ObservableList e seleziona la prima se presente. */
    public void loadSavedCards(int userId) {
        items.clear();
        try {
            CardsService.loadSavedCards(userId, items);
            if (!items.isEmpty() && table != null) {
                table.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel caricamento delle carte salvate", e);
            onError.accept("Errore nel caricamento delle carte salvate.");
        }
    }

    /**
     * Inserimento inline, con validazione e aggiornamento UI. Ritorna la Card creata (se nuova).
     */
    public void addInlineCard(
            int userId,
            TextField holderField, TextField numberField, TextField expiryField
    ) {
        CardsService.addInlineCard(
                userId, holderField, numberField, expiryField, typeCombo,
                items, table, onInfo, onError, logger
        );
    }

    /** Restituisce la carta selezionata o mostra un messaggio informativo e ritorna null. */
    public Card getSelectedOrWarn() {
        if (table == null) return null;
        Card selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) onInfo.accept("Seleziona una carta salvata.");
        return selected;
    }

    /** Restituisce il CVV valido della carta (3 cifre) o mostra un messaggio e ritorna null. */
    public String getValidCvvOrWarn(int cardId) {
        String cvv = transientCvvs.get(cardId);
        if (!CardUi.isValidCvv(cvv)) {
            onInfo.accept("Inserisci il CVV (3 cifre).");
            return null;
        }
        return cvv;
    }

    public void setProcessing(boolean value) {
        if (progress != null) progress.setVisible(value);
        if (confirmBtn != null) confirmBtn.setDisable(value);
    }

    public void close(Stage stage, Node anyNode) {
        FxUi.closeWindow(stage, anyNode);
    }
}
