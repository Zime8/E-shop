package org.example.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.OrderDAO;
import org.example.dao.SavedCardsDAO;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CartItem;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller per la schermata PaymentSelection.
 */
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
    private final SavedCardsDAO dao = new SavedCardsDAO();

    private static final Logger logger = Logger.getLogger(PaymentSelectionController.class.getName());

    private final PaymentGateway gateway = new FakePaymentGateway(800, 0.10); // 800ms delay, 10% fail

    private Stage stage;

    private Stage parentStage;
    private List<CartItem> items;
    private BigDecimal total;

    private static final String CARD_TYPE_DEBITO = "Debito";
    private static final String CARD_TYPE_CREDITO = "Credito";
    private static final String DIGITS_ONLY_REGEX = "\\D";

    public void setStage(Stage stage) { this.stage = stage; }

    public void setParentStage(Stage p){ this.parentStage = p; }

    public void setData(List<CartItem> items, BigDecimal total) {

        logger.info(" ******** DENTRO SETDATA ********");

        this.items = items;
        this.total = total;

        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);

        if (totalLabel != null && total != null) {
            totalLabel.setText("€ " + fmt.format(total));
        }
        loadSavedCards();
    }

    @FXML
    private void initialize() {
        setupTypeCombo();
        setupTableColumns();
        setupCvvColumn();
        bindTableAndWidths();
        setupSelectionHandling();
        setupConfirmEnablement();
        setupProgressIndicator();
    }

    private void setupTypeCombo() {
        if (typeCombo != null) {
            typeCombo.getItems().setAll(CARD_TYPE_DEBITO, CARD_TYPE_CREDITO);
            typeCombo.setValue(CARD_TYPE_DEBITO);
        }
    }

    private void setupTableColumns() {
        if (colId != null)      colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        if (colHolder != null)  colHolder.setCellValueFactory(cell -> cell.getValue().holderProperty());
        if (colNumber != null)  colNumber.setCellValueFactory(cell ->
                new SimpleStringProperty(maskPan(cell.getValue().getNumber())));
        if (colExpiry != null)  colExpiry.setCellValueFactory(cell -> cell.getValue().expiryProperty());
        if (colType != null)    colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
    }

    private void setupCvvColumn() {
        if (colCvv == null) return;

        // la colonna CVV è editabile e usa una cella custom con TextField e validazione
        colCvv.setEditable(true);
        colCvv.setCellValueFactory(cell -> new SimpleStringProperty("")); // nessun valore persistito
        colCvv.setCellFactory(tc -> new CvvCell());
    }

    private void bindTableAndWidths() {
        if (cardsTable == null) return;

        cardsTable.setItems(cards);
        cardsTable.setEditable(true);
        cardsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        if (colId != null)     colId.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.06));  // 6%
        if (colHolder != null) colHolder.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.34)); // 34%
        if (colNumber != null) colNumber.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.30)); // 30%
        if (colExpiry != null) colExpiry.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10)); // 10%
        if (colType != null)   colType.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10));   // 10%
        if (colCvv != null)    colCvv.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10));    // 10%
    }

    private void setupSelectionHandling() {
        if (cardsTable == null) return;

        cardsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            // disabilita Confirm se nulla selezionato
            if (confirmBtn != null) confirmBtn.setDisable(newV == null);
        });
    }

    private void setupConfirmEnablement() {
        if (confirmBtn == null) return;

        confirmBtn.setDisable(cards.isEmpty());
        cards.addListener((ListChangeListener<Card>) c ->
            confirmBtn.setDisable(cards.isEmpty() || cardsTable.getSelectionModel().getSelectedItem() == null)
        );
    }

    private void setupProgressIndicator() {
        if (progress != null) progress.setVisible(false);
    }

    private void loadSavedCards() {

        cards.clear();
        Integer userId = Session.getUserId();
        logger.log(Level.INFO, " ******** USERID ******** : {0}", userId);

        if (userId == null) return;

        try {
            logger.info(" ******** ESEGUO FINDBYUSER ********");
            List<SavedCardsDAO.Row> rows = dao.findByUser(userId);
            for (SavedCardsDAO.Row r : rows) {
                logger.info(" ******** CARTA TROVATA ********");
                Card c = new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType());
                cards.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore nel caricamente delle carte salvate", e);
            showAlert("Errore nel caricamento delle carte salvate: " + e.getMessage());
        }
    }

    @FXML
    private void onAddInline() {
        Integer userId = Session.getUserId();
        //Tanto qua probabilmente gia non ci era arrivato per blocchi sulle pagine precedenti ma ricontrollo per sicurezza
        if (userId == null) { showInfo("Devi effettuare il login"); return; }

        String holder = holderField.getText() == null ? "" : holderField.getText().trim();
        String number = numberField.getText() == null ? "" : numberField.getText().trim();
        String expiry = expiryField.getText() == null ? "" : expiryField.getText().trim();
        String type = typeCombo.getValue();

        // semplice validazione
        if (holder.isEmpty() || number.isEmpty() || expiry.isEmpty() || type == null || type.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Compila tutti i campi (titolare, numero, scadenza e tipo) ").showAndWait();
            return;
        }

        // validazione base per la carta
        if (number.replaceAll(DIGITS_ONLY_REGEX, "").length() < 12 ) {
            showInfo("Compila correttamente il seguente campo: Numero (min 12 cifre)");
            return;
        }

        if (!expiry.matches("^\\d{2}/\\d{2}$")) {
            showInfo("Compila correttamente il seguente campo:  Scadenza MM/YY");
            return;
        }

        try {
            Optional<Integer> maybeId = dao.insertIfAbsentReturningId(userId, holder, number, expiry, type);
            if (maybeId.isPresent()) {
                Card c = new Card(maybeId.get(), holder, number, expiry, type);
                //Aggiunt in cima alla tableviw
                cards.addFirst(c);
                cardsTable.getSelectionModel().select(c);
                // pulisci form per nuova inserzione
                holderField.clear(); numberField.clear(); expiryField.clear();
                if (typeCombo != null) {
                    typeCombo.setValue(CARD_TYPE_DEBITO);
                }
                showInfo("Carta aggiunta correttamente ");
            } else {
                showInfo("Carta già presente");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il salvataggio della carta", e);
            showAlert("Errore durante il salvataggio della carta: " + number);
        }
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (cardsTable != null && cardsTable.getScene() != null) ((Stage) cardsTable.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        Integer userId = Session.getUserId();
        if (userId == null) { showInfo("Devi effettuare il login."); return; }

        Card selected = getSelectedCardOrWarn();
        if (selected == null) return;

        String cvv = transientCvvs.get(selected.getId());
        if (!isValidCvv(cvv)) { showInfo("Inserisci il CVV (3 cifre) per la carta selezionata."); return; }

        setProcessing(true);

        Map<String, String> paymentData = preparePaymentData(selected, cvv);
        Task<OrderDAO.CreationResult> task = getCreationResultTask(userId, paymentData, selected);

        new Thread(task).start();
    }

    private Task<OrderDAO.CreationResult> getCreationResultTask(Integer userId, Map<String, String> paymentData, Card selected) {
        Task<OrderDAO.CreationResult> task = buildPaymentTask(userId, paymentData);

        task.setOnSucceeded(evt -> handlePaymentSuccess(selected.getId(), task.getValue()));
        task.setOnFailed(evt -> handlePaymentFailure(task.getException()));
        return task;
    }

    private Card getSelectedCardOrWarn() {
        Card selected = cardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Seleziona una carta salvata per procedere al pagamento oppure aggiungine una");
        }
        return selected;
    }

    private boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3}");
    }

    private Map<String, String> preparePaymentData(Card selected, String cvv) {
        Map<String, String> paymentData = new HashMap<>();
        paymentData.put("card_number", selected.getNumber());
        paymentData.put("expiry", selected.getExpiry());
        paymentData.put("cvv", cvv);
        logger.log(Level.FINE, "** CVV present: {0}", (cvv != null && !cvv.isBlank()) ? "***" : "no");
        return paymentData;
    }

    private Task<OrderDAO.CreationResult> buildPaymentTask(Integer userId, Map<String, String> paymentData) {
        return new Task<>() {
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
    }

    private void handlePaymentSuccess(int cardId, OrderDAO.CreationResult res) {
        transientCvvs.remove(cardId);
        Session.clearCart();

        Platform.runLater(() -> {
            setProcessing(false);

            // chiudi riepilogo ordine
            if (parentStage != null) parentStage.close();

            if (onCartUpdated != null) onCartUpdated.run();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Ordine completato");
            ok.setHeaderText("ID ordine" + res.orderIds());
            ok.setContentText("Grazie! Il pagamento è andato a buon fine.");
            ok.showAndWait();

            if (stage != null) stage.close();
        });
    }

    private void setProcessing(boolean processing) {
        if (progress != null) progress.setVisible(processing);
        if (confirmBtn != null) confirmBtn.setDisable(processing);
        if (backBtn != null) backBtn.setDisable(processing);
    }


    private String maskPan(String pan) {
        if (pan == null) return "";
        String digits = pan.replaceAll(DIGITS_ONLY_REGEX, "");
        if (digits.length() <= 4) return digits;
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
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

    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }

    private class CvvCell extends TableCell<Card, String> {
        private final TextField tf = new TextField();

        CvvCell() {
            tf.setPromptText("CVV");
            tf.setPrefWidth(70);

            // accetta solo cifre, max 3
            tf.textProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                String digits = newV.replaceAll(DIGITS_ONLY_REGEX, "");
                if (!digits.equals(newV)) {
                    tf.setText(digits);
                    return;
                }
                if (digits.length() > 3) {
                    tf.setText(digits.substring(0, 3));
                }
            });

            // salva il CVV temporaneo per la riga corrente
            tf.textProperty().addListener((obs, oldV, newV) -> {
                Card row = getCurrentRowCard();
                if (row == null) return;
                if (newV == null || newV.isEmpty()) transientCvvs.remove(row.getId());
                else transientCvvs.put(row.getId(), newV);
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
                return;
            }
            Card row = getCurrentRowCard();
            if (row == null) {
                setGraphic(null);
                return;
            }
            tf.setText(transientCvvs.getOrDefault(row.getId(), ""));
            boolean selected = cardsTable.getSelectionModel().getSelectedItem() == row;
            tf.setDisable(!selected);
            setGraphic(tf);
        }

        private Card getCurrentRowCard() {
            return (getTableRow() == null) ? null : getTableRow().getItem();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void handlePaymentFailure(Throwable e) {
        logger.log(Level.SEVERE, "Errore durante il pagamento: " + e.getMessage());
        Platform.runLater(() -> {
            setProcessing(false);
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = "Errore sconosciuto durante il pagamento/ordine.";
            showError(msg);
        });
    }
}
