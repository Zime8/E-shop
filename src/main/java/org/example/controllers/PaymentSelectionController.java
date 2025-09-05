package org.example.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.OrderDAO;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CartItem;
import org.example.services.CardsService;
import org.example.ui.CardUi;
import org.example.util.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentSelectionController {

    @FXML private TableView<Card> cardsTable;
    @FXML private TableColumn<Card, Number> colId;
    @FXML private TableColumn<Card, String> colHolder;
    @FXML private TableColumn<Card, String> colNumber;
    @FXML private TableColumn<Card, String> colExpiry;
    @FXML private TableColumn<Card, String> colType;
    @FXML private TableColumn<Card, String> colCvv;

    private Runnable onCartUpdated;

    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();
    @FXML private TextField holderField;
    @FXML private TextField numberField;
    @FXML private TextField expiryField;

    @FXML private Button backBtn;
    @FXML private Button confirmBtn;

    @FXML private Label totalLabel;
    @FXML private ProgressIndicator progress;

    @FXML private ComboBox<String> typeCombo;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();

    private static final Logger logger = Logger.getLogger(PaymentSelectionController.class.getName());

    private final PaymentGateway gateway = new FakePaymentGateway(800, 0.10); // 800ms delay, 10% fail

    private Stage stage;
    private Stage parentStage;
    private List<CartItem> items;
    private BigDecimal total;

    public void setStage(Stage stage) { this.stage = stage; }
    public void setParentStage(Stage p){ this.parentStage = p; }

    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }

    public void setData(List<CartItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;

        NumberFormat fmt = NumberFormat.getCurrencyInstance(java.util.Locale.ITALY);
        if (totalLabel != null && total != null) {
            totalLabel.setText(fmt.format(total));
        }
        loadSavedCards();
    }

    @FXML
    private void initialize() {
        CardUi.setupTypeCombo(typeCombo);
        CardUi.initCardTable(cardsTable, cards, colId, colHolder, colNumber, colExpiry, colType, colCvv, transientCvvs);
        CardUi.bindConfirmEnablement(cards, cardsTable, confirmBtn);
        setupProgressIndicator();
    }

    private void setupProgressIndicator() {
        if (progress != null) progress.setVisible(false);
    }

    private void loadSavedCards() {
        cards.clear();
        Integer userId = Session.getUserId();
        logger.log(Level.INFO, "UserId corrente: {0}", userId);
        if (userId == null) return;

        try {
            CardsService.loadSavedCards(userId, cards);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nel caricamento delle carte salvate", e);
            showAlert("Errore nel caricamento delle carte salvate: " + e.getMessage());
        }
    }

    @FXML
    private void onAddInline() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) { showInfo("Devi effettuare il login"); return; }

        CardsService.addInlineCard(
                currentUserId,
                holderField, numberField, expiryField, typeCombo,
                cards, cardsTable
        );
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (cardsTable != null && cardsTable.getScene() != null)
            ((Stage) cardsTable.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        Integer userId = Session.getUserId();
        if (userId == null) { showInfo("Devi effettuare il login."); return; }

        Card selected = cardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Seleziona una carta salvata per procedere."); return; }

        String cvv = transientCvvs.get(selected.getId());
        if (CardUi.isValidCvv(cvv)) { showInfo("Inserisci il CVV (3 cifre) per la carta selezionata."); return; }

        setProcessing(true);

        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("card_number", selected.getNumber());
        paymentData.put("expiry",      selected.getExpiry());
        paymentData.put("cvv",         cvv);
        logger.log(Level.FINE, "CVV presente: {0}", !cvv.isBlank() ? "***" : "no");

        Task<OrderDAO.CreationResult> task = getCreationResultTask(userId, paymentData, selected);
        new Thread(task).start();
    }

    private Task<OrderDAO.CreationResult> getCreationResultTask(Integer userId, Map<String, String> paymentData, Card selected) {
        Task<OrderDAO.CreationResult> task = new Task<>() {
            @Override
            protected OrderDAO.CreationResult call() throws Exception {
                PaymentResult payRes = gateway.charge(userId, total, paymentData);
                if (!payRes.success()) {
                    throw new IllegalStateException("Pagamento rifiutato: " + payRes.message());
                }
                OrderDAO.CreationResult res = OrderDAO.placeOrderWithStockDecrement(userId, items);
                logger.log(Level.INFO, "Payment txId: {0}", payRes.transactionId());
                return res;
            }
        };

        task.setOnSucceeded(evt -> handlePaymentSuccess(selected.getId(), task.getValue()));
        task.setOnFailed(evt -> handlePaymentFailure(task.getException()));
        return task;
    }

    private void handlePaymentSuccess(int cardId, OrderDAO.CreationResult res) {
        transientCvvs.remove(cardId);
        Session.clearCart();

        Platform.runLater(() -> {
            setProcessing(false);
            if (parentStage != null) parentStage.close();
            if (onCartUpdated != null) onCartUpdated.run();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Ordine completato");
            ok.setHeaderText("ID ordine: " + res.orderIds());
            ok.setContentText("Grazie! Il pagamento è andato a buon fine.");
            ok.showAndWait();

            if (stage != null) stage.close();
        });
    }

    private void handlePaymentFailure(Throwable e) {
        Logger.getLogger(PaymentSelectionController.class.getName())
                .log(Level.SEVERE, "Errore durante il pagamento: {0}", e != null ? e.getMessage() : "sconosciuto");
        Platform.runLater(() -> {
            setProcessing(false);
            String msg = (e == null || e.getMessage() == null || e.getMessage().isBlank())
                    ? "Errore sconosciuto durante il pagamento/ordine."
                    : e.getMessage();
            showError(msg);
        });
    }

    private void setProcessing(boolean processing) {
        if (progress != null) progress.setVisible(processing);
        if (confirmBtn != null) confirmBtn.setDisable(processing);
        if (backBtn != null)    backBtn.setDisable(processing);
    }

    private void showInfo(String s) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, s);
        a.setHeaderText(null);
        if (stage != null) a.initOwner(stage);
        a.showAndWait();
    }
    private void showError(String s) {
        Alert a = new Alert(Alert.AlertType.ERROR, s);
        a.setHeaderText(null);
        if (stage != null) a.initOwner(stage);
        a.showAndWait();
    }
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
