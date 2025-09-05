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
import org.example.dao.SavedCardsDAO;
import org.example.dao.ShopDAO;
import org.example.models.Card;
import org.example.util.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithdrawSelectionController {

    @FXML private TableView<Card> cardsTable;
    @FXML private TableColumn<Card, Number> colId;
    @FXML private TableColumn<Card, String> colHolder;
    @FXML private TableColumn<Card, String> colNumber;
    @FXML private TableColumn<Card, String> colExpiry;
    @FXML private TableColumn<Card, String> colType;
    @FXML private TableColumn<Card, String> colCvv;

    @FXML private TextField holderField;
    @FXML private TextField numberField;
    @FXML private TextField expiryField;
    @FXML private ComboBox<String> typeCombo;

    @FXML private Label availableLabel;
    @FXML private TextField amountField;
    @FXML private Button backBtn;
    @FXML private Button confirmBtn;
    @FXML private ProgressIndicator progress;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(WithdrawSelectionController.class.getName());

    private static final String CARD_TYPE_DEBITO = "Debito";
    private static final String CARD_TYPE_CREDITO = "Credito";
    private static final String DIGITS_ONLY_REGEX = "\\D";

    private Stage stage;
    private Runnable onWithdrawDone;

    private BigDecimal available = BigDecimal.ZERO;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);

    public void setStage(Stage stage) { this.stage = stage; }
    public void setOnWithdrawDone(Runnable r) { this.onWithdrawDone = r; }

    @FXML
    private void initialize() {
        setupTypeCombo();
        setupTableColumns();
        setupCvvColumn();
        bindTableAndWidths();
        setupSelectionHandling();
        setupConfirmEnablement();
        setupProgressIndicator();
        loadData();
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
        colCvv.setEditable(true);
        colCvv.setCellValueFactory(cell -> new SimpleStringProperty(""));
        colCvv.setCellFactory(tc -> new CvvCell());
    }

    private void bindTableAndWidths() {
        if (cardsTable == null) return;
        cardsTable.setItems(cards);
        cardsTable.setEditable(true);
        cardsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        if (colId != null)     colId.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.06));
        if (colHolder != null) colHolder.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.34));
        if (colNumber != null) colNumber.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.30));
        if (colExpiry != null) colExpiry.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10));
        if (colType != null)   colType.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10));
        if (colCvv != null)    colCvv.prefWidthProperty().bind(cardsTable.widthProperty().multiply(0.10));
    }

    private void setupSelectionHandling() {
        if (cardsTable == null) return;
        cardsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
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

    private void loadData() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) return;

        try {
            // saldo disponibile
            available = ShopDAO.getBalance(currentUserId);
            availableLabel.setText("€ " + currency.format(available));

            // carte salvate dell'utente (venditore)
            cards.clear();
            List<SavedCardsDAO.Row> rows = SavedCardsDAO.findByUser(currentUserId);
            for (SavedCardsDAO.Row r : rows) {
                Card c = new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType());
                cards.add(c);
            }
            if (!cards.isEmpty()) cardsTable.getSelectionModel().selectFirst();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore caricamento dati prelievo", e);
            showAlert("Errore nel caricamento: " + e.getMessage());
        }
    }

    @FXML
    private void onAddInline() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) { showInfo("Devi effettuare il login"); return; }

        String holder = safe(holderField);
        String number = safe(numberField);
        String expiry = safe(expiryField);
        String type   = (typeCombo != null) ? typeCombo.getValue() : null;

        if (holder.isEmpty() || number.isEmpty() || expiry.isEmpty() || type == null || type.isBlank()) {
            showInfo("Compila tutti i campi (titolare, numero, scadenza e tipo).");
            return;
        }
        if (number.replaceAll(DIGITS_ONLY_REGEX, "").length() < 12 ) {
            showInfo("Compila correttamente il Numero (min 12 cifre).");
            return;
        }
        if (!expiry.matches("^\\d{2}/\\d{2}$")) {
            showInfo("Compila correttamente la Scadenza (MM/YY).");
            return;
        }

        try {
            Optional<Integer> maybeId = SavedCardsDAO.insertIfAbsentReturningId(currentUserId, holder, number, expiry, type);
            if (maybeId.isPresent()) {
                Card c = new Card(maybeId.get(), holder, number, expiry, type);
                cards.addFirst(c);
                cardsTable.getSelectionModel().select(c);
                holderField.clear(); numberField.clear(); expiryField.clear();
                if (typeCombo != null) typeCombo.setValue(CARD_TYPE_DEBITO);
                showInfo("Carta aggiunta correttamente.");
            } else {
                showInfo("Carta già presente.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore salvataggio carta", e);
            showAlert("Errore durante il salvataggio della carta.");
        }
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (cardsTable != null && cardsTable.getScene() != null)
            ((Stage) cardsTable.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        Integer currentUserId = Session.getUserId();
        if (currentUserId == null) { showInfo("Devi effettuare il login."); return; }

        Card selected = cardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Seleziona una carta salvata."); return; }

        String cvv = transientCvvs.get(selected.getId());
        if (cvv == null || !cvv.matches("\\d{3}")) { showInfo("Inserisci il CVV (3 cifre)."); return; }

        BigDecimal amount;
        try {
            String raw = safe(amountField).replace(".", "").replace(',', '.'); // accetta "1.234,56"
            amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            showInfo("Importo non valido."); return;
        }
        if (amount.signum() <= 0 || amount.compareTo(available) > 0) {
            showInfo("Importo non valido ( importo disponibile : " + currency.format(available) + " )");
            return;
        }

        setProcessing(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ShopDAO.requestWithdraw(currentUserId, selected.getId(), amount, cvv);
                return null;
            }
        };

        task.setOnSucceeded(evt -> Platform.runLater(() -> {
            setProcessing(false);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Prelievo effettuato: € " + currency.format(amount));
            ok.setHeaderText(null);
            ok.showAndWait();
            if (onWithdrawDone != null) onWithdrawDone.run(); // per ricaricare saldo nell'header padre
            onBack();
        }));

        task.setOnFailed(evt -> Platform.runLater(() -> {
            setProcessing(false);
            String msg = (task.getException() == null) ? "Errore sconosciuto" : task.getException().getMessage();
            showError(msg);
        }));

        new Thread(task).start();
    }

    private void setProcessing(boolean processing) {
        if (progress != null) progress.setVisible(processing);
        if (confirmBtn != null) confirmBtn.setDisable(processing);
        if (backBtn != null) backBtn.setDisable(processing);
    }

    // ===== util =====
    private String maskPan(String pan) {
        if (pan == null) return "";
        String digits = pan.replaceAll(DIGITS_ONLY_REGEX, "");
        if (digits.length() <= 4) return digits;
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }
    private String safe(TextField tf) { return tf == null || tf.getText() == null ? "" : tf.getText().trim(); }

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

    private class CvvCell extends TableCell<Card, String> {
        private final TextField tf = new TextField();

        CvvCell() {
            tf.setPromptText("CVV");
            tf.setPrefWidth(70);

            tf.textProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                String digits = newV.replaceAll(DIGITS_ONLY_REGEX, "");
                if (!digits.equals(newV)) { tf.setText(digits); return; }
                if (digits.length() > 3) tf.setText(digits.substring(0, 3));
            });

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
            if (empty) { setGraphic(null); return; }
            Card row = getCurrentRowCard();
            if (row == null) { setGraphic(null); return; }
            tf.setText(transientCvvs.getOrDefault(row.getId(), ""));
            boolean selected = cardsTable.getSelectionModel().getSelectedItem() == row;
            tf.setDisable(!selected);
            setGraphic(tf);
        }
        private Card getCurrentRowCard() {
            return (getTableRow() == null) ? null : getTableRow().getItem();
        }
    }
    public void setUserId(long sellerUserId) {
        if (Session.getUserId() == null) {
            Session.setUserId((int) sellerUserId);
        }
        if (availableLabel != null) {
            loadData();
        }
    }
}
