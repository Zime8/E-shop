package org.example.boundary;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.control.Withdraw;
import org.example.ui.CardViewModel;
import org.example.ui.CardUi;

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

    private Withdraw appController;
    private Runnable onSuccess;
    private Integer userId;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);

    public void setAppController(Withdraw app) {
        this.appController = app;
        if (userId != null){
            refreshData();
        }
    }

    public void setUserId(int userId) {
        this.userId = userId;
        if (appController != null){
            refreshData();
        }
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
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
    }

    private void refreshData() {
        if (appController == null || userId == null) return;

        availableLabel.setText(currency.format(appController.loadBalance(userId)));

        cards.setAll(appController.loadSavedCards(userId).stream()
                .map(CardViewModel::new)
                .toList());
    }

    private void setupProgressIndicator() {
        if (progress != null) progress.setVisible(false);
    }

    @FXML
    private void onAddInline() {
        if (appController == null || userId == null) {
            showError("Utente non disponibile.");
            return;
        }

        String holder = holderField.getText().trim();
        String number = numberField.getText().trim();
        String expiry = expiryField.getText().trim();
        String type = typeCombo.getValue();

        if (holder.isBlank() || number.isBlank() || expiry.isBlank() || type == null) {
            showInfo("Compila tutti i campi carta.");
            return;
        }

        appController.addInlineCardAsync(userId, holder, number, expiry, type,
                this::refreshData,
                this::showError);
        clearInlineFields();
    }

    @FXML
    private void onBack() {
        closeWindow();
    }

    @FXML private void onConfirm() {
        if (appController == null || userId == null) {
            showError("Utente non disponibile.");
            return;
        }

        CardViewModel selected = cardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Seleziona una carta salvata.");
            return;
        }

        String cvv = transientCvvs.get(selected.getId());
        if (cvv == null || !cvv.matches("\\d{3}")) {
            showInfo("Inserisci CVV valido (3 cifre).");
            return;
        }

        BigDecimal amount;
        try {
            String raw = amountField.getText().trim().replace(",", ".");
            amount = new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                showInfo("Importo positivo.");
                return;
            }
        } catch (NumberFormatException e) {
            showInfo("Importo non valido.");
            return;
        }

        var selectedCard = selected.toDto();

        setProcessing(true);
        appController.confirmWithdrawAsync(userId, selectedCard, cvv, amount,
                () -> {
                    setProcessing(false);
                    showInfo("Prelievo OK: " + currency.format(amount));
                    if (onSuccess != null) onSuccess.run();
                    closeWindow();
                },
                err -> {
                    setProcessing(false);
                    showError(err);
                });
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

    private void closeWindow() {
        if (cardsTable != null && cardsTable.getScene() != null) {
            Stage stage = (Stage) cardsTable.getScene().getWindow();
            stage.close();
        }
    }

    private void showInfo(String s) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, s);
        a.setHeaderText(null);
        if (cardsTable != null && cardsTable.getScene() != null) {
            a.initOwner(cardsTable.getScene().getWindow());
        }
        a.showAndWait();
    }

    private void showError(String s) {
        Alert a = new Alert(Alert.AlertType.ERROR, s);
        a.setHeaderText(null);
        if (cardsTable != null && cardsTable.getScene() != null) {
            a.initOwner(cardsTable.getScene().getWindow());
        }
        a.showAndWait();
    }
}
