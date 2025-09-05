package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controllers.support.CardScreenHelper;
import org.example.dao.ShopDAO;
import org.example.models.Card;
import org.example.util.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithdrawSelectionController {

    // Table & colonne
    @FXML private TableView<Card> cardsTable;
    @FXML private TableColumn<Card, Number> colId;
    @FXML private TableColumn<Card, String> colHolder;
    @FXML private TableColumn<Card, String> colNumber;
    @FXML private TableColumn<Card, String> colExpiry;
    @FXML private TableColumn<Card, String> colType;
    @FXML private TableColumn<Card, String> colCvv;

    // Form per aggiunta rapida
    @FXML private TextField holderField;
    @FXML private TextField numberField;
    @FXML private TextField expiryField;
    @FXML private ComboBox<String> typeCombo;

    // Importo & azioni
    @FXML private Label availableLabel;
    @FXML private TextField amountField;
    @FXML private Button confirmBtn;
    @FXML private ProgressIndicator progress;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();
    private CardScreenHelper helper;

    private static final Logger logger = Logger.getLogger(WithdrawSelectionController.class.getName());

    private Stage stage;
    private Runnable onWithdrawDone;

    private BigDecimal available = BigDecimal.ZERO;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);

    public void setStage(Stage stage) { this.stage = stage; }
    public void setOnWithdrawDone(Runnable r) { this.onWithdrawDone = r; }

    @FXML
    private void initialize() {
        // helper UI condiviso (niente più codice duplicato)
        helper = new CardScreenHelper(
                cardsTable, colId, colHolder, colNumber, colExpiry, colType, colCvv,
                typeCombo, cards, transientCvvs,
                confirmBtn, progress,
                logger,
                this::showInfo, this::showAlert
        );
        helper.initUi();
        loadData();
    }

    private void loadData() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) return;

        try {
            // saldo disponibile
            available = ShopDAO.getBalance(currentUserId);
            availableLabel.setText(currency.format(available));

            // carte salvate
            helper.loadSavedCards(currentUserId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento dati prelievo", e);
            showAlert("Errore nel caricamento: " + e.getMessage());
        }
    }

    @FXML
    private void onAddInline() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) { showInfo("Devi effettuare il login"); return; }
        helper.addInlineCard(currentUserId, holderField, numberField, expiryField);
    }

    @FXML
    private void onBack() {
        helper.close(stage, cardsTable);
    }

    @FXML
    private void onConfirm() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) { showInfo("Devi effettuare il login."); return; }

        Card selected = helper.getSelectedOrWarn();
        if (selected == null) return;

        String cvv = helper.getValidCvvOrWarn(selected.getId());
        if (cvv == null) return; // messaggio già mostrato

        BigDecimal amount;
        try {
            String raw = (amountField.getText() == null ? "" : amountField.getText().trim())
                    .replace(".", "").replace(',', '.'); // accetta "1.234,56"
            amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            showInfo("Importo non valido."); return;
        }
        if (amount.signum() <= 0 || amount.compareTo(available) > 0) {
            showInfo("Importo non valido (disponibile: " + currency.format(available) + ").");
            return;
        }

        helper.setProcessing(true);

        Task<Void> task = getTask(currentUserId, amount);

        new Thread(task).start();
    }

    private Task<Void> getTask(Integer currentUserId, BigDecimal amount) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ShopDAO.requestWithdraw(currentUserId, amount);
                return null;
            }
        };

        task.setOnSucceeded(evt -> Platform.runLater(() -> {
            helper.setProcessing(false);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Prelievo effettuato: " + currency.format(amount));
            ok.setHeaderText(null);
            ok.showAndWait();
            if (onWithdrawDone != null) onWithdrawDone.run();
            onBack();
        }));

        task.setOnFailed(evt -> Platform.runLater(() -> {
            helper.setProcessing(false);
            String msg = (task.getException() == null) ? "Errore sconosciuto" : task.getException().getMessage();
            showError(msg);
        }));
        return task;
    }

    // ===== util UI locali =====
    private void showInfo(String s) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, s);
        a.setHeaderText(null);
        if (stage != null) a.initOwner(stage);
        a.showAndWait();
    }
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    private void showError(String s) {
        Alert a = new Alert(Alert.AlertType.ERROR, s);
        a.setHeaderText(null);
        if (stage != null) a.initOwner(stage);
        a.showAndWait();
    }

    // (opzionale) compat: set da chiamate esterne
    public void setUserId(long sellerUserId) {
        if (Session.getUserId() == null) {
            Session.setUserId((int) sellerUserId);
        }
        if (availableLabel != null) {
            loadData();
        }
    }
}
