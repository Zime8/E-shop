package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.controllers.app.WithdrawSelectionAppController;
import org.example.controllers.control.WithdrawSelectionControl;
import org.example.models.CardViewModel;
import org.example.ui.CardUi;
import org.example.util.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WithdrawSelectionController {

    @FXML private TableView<CardViewModel> cardsTable;
    @FXML private TableColumn<CardViewModel, Number> colId;
    @FXML private TableColumn<CardViewModel, String> colHolder;
    @FXML private TableColumn<CardViewModel, String> colNumber;
    @FXML private TableColumn<CardViewModel, String> colExpiry;
    @FXML private TableColumn<CardViewModel, String> colType;
    @FXML private TableColumn<CardViewModel, String> colCvv;

    @FXML private TextField holderField;
    @FXML private TextField numberField;
    @FXML private TextField expiryField;
    @FXML private ComboBox<String> typeCombo;

    @FXML private Label availableLabel;
    @FXML private TextField amountField;
    @FXML private Button backBtn;
    @FXML private Button confirmBtn;
    @FXML private ProgressIndicator progress;

    private final ObservableList<CardViewModel> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();

    private Stage stage;
    private WithdrawSelectionControl control;
    private Runnable onWithdrawDone;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);

    public void setStage(Stage stage) { this.stage = stage; }

    public void setOnWithdrawDone(Runnable r) {
        this.onWithdrawDone = r;
    }

    @FXML
    private void initialize() {
        CardUi.setupTypeCombo(typeCombo);
        CardUi.initCardTable(
                new CardUi.CardTableContext(cardsTable, cards, transientCvvs),
                new CardUi.CardColumns(colId, colHolder, colNumber, colExpiry, colType, colCvv)
        );
        CardUi.bindConfirmEnablement(cards, cardsTable, confirmBtn);
        setupProgressIndicator();

        control = new WithdrawSelectionAppController();  // ← Control BCE
        loadData();
    }

    private void setupProgressIndicator() {
        if (progress != null) progress.setVisible(false);
    }

    private void loadData() {
        availableLabel.setText(currency.format(control.loadBalance()));
        cards.clear();
        cards.addAll(control.loadSavedCards());  // ← Solo Control!
    }

    @FXML
    private void onAddInline() {
        String holder = holderField.getText().trim();
        String number = numberField.getText().trim();
        String expiry = expiryField.getText().trim();
        String type = typeCombo.getValue();

        if (holder.isBlank() || number.isBlank() || expiry.isBlank() || type == null) {
            showInfo("Compila tutti i campi carta.");
            return;
        }

        control.addInlineCard(holder, number, expiry, type);
        clearInlineFields();
        loadData();  // Refresh lista
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (cardsTable != null && cardsTable.getScene() != null)
            ((Stage) cardsTable.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        CardViewModel selected = cardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Seleziona una carta salvata.");
            return;
        }

        String cvv = transientCvvs.get(selected.getId());
        if (cvv == null || !CardUi.isValidCvv(cvv)) {
            showInfo("Inserisci il CVV (3 cifre).");
            return;
        }

        BigDecimal amount;
        try {
            String raw = amountField.getText().trim().replace(",", ".");
            amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            showInfo("Importo non valido.");
            return;
        }

        if (amount.signum() <= 0) {
            showInfo("Importo deve essere positivo.");
            return;
        }

        setProcessing(true);
        new Thread(() -> {
            try {
                control.confirmWithdraw(amount, selected.toEntity(), cvv);
                Platform.runLater(() -> {
                    setProcessing(false);
                    showInfo("Prelievo effettuato: " + currency.format(amount));
                    if (onWithdrawDone != null) onWithdrawDone.run();
                    if (stage != null) stage.close();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setProcessing(false);
                    showError("Errore prelievo: " + e.getMessage());
                });
            }
        }).start();
    }

    public void setUserId(long sellerUserId) {
        if (Session.getUserId() == null) {
            Session.setUserId((int) sellerUserId);
        }
        loadData();
    }

    private void clearInlineFields() {
        holderField.clear();
        numberField.clear();
        expiryField.clear();
        typeCombo.setValue("Debito");
    }

    private void setProcessing(boolean processing) {
        if (progress != null) progress.setVisible(processing);
        if (confirmBtn != null) confirmBtn.setDisable(processing);
        if (backBtn != null) backBtn.setDisable(processing);
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
}
