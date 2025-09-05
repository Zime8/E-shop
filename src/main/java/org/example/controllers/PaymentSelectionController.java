package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controllers.support.CardScreenHelper;
import org.example.dao.OrderDAO;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CartItem;
import org.example.util.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentSelectionController {

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

    // Azioni / UI
    @FXML private Button confirmBtn;
    @FXML private Label totalLabel;
    @FXML private ProgressIndicator progress;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();
    private CardScreenHelper helper;

    private static final Logger logger = Logger.getLogger(PaymentSelectionController.class.getName());

    private final PaymentGateway gateway = new FakePaymentGateway(800, 0.10); // 800ms, 10% fail

    private Stage stage;
    private Stage parentStage;
    private List<CartItem> items;
    private BigDecimal total;

    private Runnable onCartUpdated;

    public void setStage(Stage stage) { this.stage = stage; }
    public void setParentStage(Stage p){ this.parentStage = p; }
    public void setOnCartUpdated(Runnable onCartUpdated) { this.onCartUpdated = onCartUpdated; }

    @FXML
    private void initialize() {
        helper = new CardScreenHelper(
                cardsTable, colId, colHolder, colNumber, colExpiry, colType, colCvv,
                typeCombo, cards, transientCvvs,
                confirmBtn, progress,
                logger,
                this::showInfo, this::showAlert
        );
        helper.initUi();
        // Le carte verranno caricate quando arriva setData(...)
    }

    /** Dati dal riepilogo: carrello + totale. */
    public void setData(List<CartItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;

        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        if (totalLabel != null && total != null) {
            totalLabel.setText("€ " + fmt.format(total));
        }

        Integer userId = Session.getUserId();
        if (userId != null) {
            helper.loadSavedCards(userId);
        }
    }

    @FXML
    private void onAddInline() {
        Integer userId = Session.getUserId();
        if (userId == null) { showInfo("Devi effettuare il login"); return; }
        helper.addInlineCard(userId, holderField, numberField, expiryField);
    }

    @FXML
    private void onBack() {
        helper.close(stage, cardsTable);
    }

    @FXML
    private void onConfirm() {
        Integer userId = Session.getUserId();
        if (userId == null) { showInfo("Devi effettuare il login."); return; }

        Card selected = helper.getSelectedOrWarn();
        if (selected == null) return;

        String cvv = helper.getValidCvvOrWarn(selected.getId());
        if (cvv == null) return; // messaggio già mostrato

        helper.setProcessing(true);

        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("card_number", selected.getNumber());
        paymentData.put("expiry", selected.getExpiry());
        paymentData.put("cvv", cvv);
        logger.log(Level.FINE, "** CVV present: {0}", !cvv.isBlank() ? "***" : "no");

        Task<OrderDAO.CreationResult> task = new Task<>() {
            @Override
            protected OrderDAO.CreationResult call() throws Exception {
                PaymentResult payRes = gateway.charge(userId, total, paymentData);
                if (!payRes.success()) {
                    throw new IllegalStateException("Pagamento rifiutato: " + payRes.message());
                }
                return OrderDAO.placeOrderWithStockDecrement(userId, items);
            }
        };

        task.setOnSucceeded(evt -> Platform.runLater(() -> {
            helper.setProcessing(false);
            transientCvvs.remove(selected.getId());
            Session.clearCart();

            if (parentStage != null) parentStage.close();
            if (onCartUpdated != null) onCartUpdated.run();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Ordine completato");
            ok.setHeaderText("ID ordine " + task.getValue().orderIds());
            ok.setContentText("Grazie! Il pagamento è andato a buon fine.");
            ok.showAndWait();

            onBack();
        }));

        task.setOnFailed(evt -> Platform.runLater(() -> {
            helper.setProcessing(false);
            String msg = task.getException() != null ? task.getException().getMessage()
                    : "Errore sconosciuto durante il pagamento/ordine.";
            showError(msg);
        }));

        new Thread(task).start();
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
}
