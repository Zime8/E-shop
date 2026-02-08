package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;
import org.example.models.InlineCardData;
import org.example.control.services.CardsService;
import org.example.ui.CardUi;
import org.example.util.CardValidator;
import org.example.util.Session;
import org.example.controllers.app.PaymentSelectionAppController;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaymentSelectionController {

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
    @FXML private TextField addressField;

    @FXML private Button backBtn;
    @FXML private Button confirmBtn;

    @FXML private Label totalLabel;
    @FXML private ProgressIndicator progress;

    @FXML private ComboBox<String> typeCombo;

    private PaymentSelectionAppController appController;

    private final ObservableList<CardViewModel> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();

    private Stage stage;
    private Stage parentStage;
    private List<CartItem> items;
    private BigDecimal total;

    public void setAppController(Object app) {
        this.appController = (PaymentSelectionAppController) app;
    }

    public void setStage(Stage stage) { this.stage = stage; }
    public void setParentStage(Stage p){ this.parentStage = p; }

    private Runnable onCartUpdated;

    @SuppressWarnings("unused") // Navigator
    public void setOnCartUpdated(Runnable onCartUpdated) {
        this.onCartUpdated = onCartUpdated;
    }

    @SuppressWarnings("unused") // Navigator
    public void loadData(Object dataObj) {
        if (dataObj instanceof Object[] arr && arr.length >= 2) {
            @SuppressWarnings("unchecked")
            List<CartItem> dataItems = (List<CartItem>) arr[0];
            BigDecimal dataTotal = (BigDecimal) arr[1];
            if (arr.length > 2 && arr[2] instanceof Runnable callback) {
                this.onCartUpdated = callback;
            }
            setData(dataItems, dataTotal);
        }
    }

    // Carica i
    public void setData(List<CartItem> items, BigDecimal total) {
        this.items = items;
        this.total = total;

        if (appController != null) {
            appController.loadUserData(Session.getUserId());
        }

        cards.clear();
        cards.addAll(appController.loadSavedCards());

        NumberFormat fmt = NumberFormat.getCurrencyInstance(java.util.Locale.ITALY);
        if (totalLabel != null && total != null) {
            totalLabel.setText(fmt.format(total));
        }
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

    private void setupProgressIndicator() {
        if (progress != null) progress.setVisible(false);
    }

    @FXML
    private void onBack() {
        if (stage != null) stage.close();
        else if (cardsTable != null && cardsTable.getScene() != null)
            ((Stage) cardsTable.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        String address = (addressField != null) ? addressField.getText() : null;

        CardViewModel selectedVM = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedVM == null) {
            showInfo("Seleziona una carta salvata per procedere.");
            return;
        }

        Card selected = selectedVM.toEntity();
        String cvv = transientCvvs.get(selected.id());
        if (!CardValidator.isValidCvv(cvv)) {
            showInfo("Inserisci il CVV (3 cifre) per la carta selezionata.");
            return;
        }

        if (address == null || address.isBlank()) {
            showInfo("Inserisci l'indirizzo di spedizione.");
            return;
        }

        setProcessing(true);

        new Thread(() -> {
            try {
                PaymentResult result = appController.confirmPayment(selected, cvv, address, items, total);
                Platform.runLater(() -> {
                    setProcessing(false);
                    if (result.success()) {
                        onPaymentSuccess();
                    } else {
                        showError(result.message());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setProcessing(false);
                    showError("Errore: " + e.getMessage());
                });
            }
        }).start();
    }

    private void onPaymentSuccess() {
        transientCvvs.clear();
        Session.clearCart();

        if (parentStage != null) parentStage.close();
        if (onCartUpdated != null) onCartUpdated.run();

        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Ordine completato");
        ok.setHeaderText("ID Ordine: " + appController.getLastOrderIds());
        ok.setContentText("Grazie! Pagamento ok");
        if (stage != null) ok.initOwner(stage);
        ok.showAndWait();

        if (stage != null) stage.close();
    }

    @FXML private void onAddInline() {
        InlineCardData data = new InlineCardData(
                holderField.getText().trim(), numberField.getText().trim(),
                expiryField.getText().trim(), typeCombo.getValue()
        );

        CardsService.AddCardResult result = CardsService.addInlineCard(appController.getUserId(), data);

        if (result.ok()) {
            CardViewModel newCardVM = new CardViewModel(result.card());
            cards.add(newCardVM);
            cardsTable.getSelectionModel().select(newCardVM);
            clearFields();
        } else {
            showInfo(result.message());
        }
    }

    private void clearFields() {
        holderField.clear(); numberField.clear(); expiryField.clear();
        typeCombo.setValue("Debito");  // Default
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
}
