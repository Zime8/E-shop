package org.example.boundary;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.control.BuyProductController;
import org.example.models.dto.Card;
import org.example.models.dto.CheckoutRequest;
import org.example.models.dto.CheckoutResult;
import org.example.models.dto.InlineCardData;
import org.example.ui.CardViewModel;
import org.example.ui.CardUi;
import org.example.util.CardValidator;

import java.math.BigDecimal;
import java.text.NumberFormat;
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

    private BuyProductController appController;

    private final ObservableList<CardViewModel> cards = FXCollections.observableArrayList();
    private final Map<Integer, String> transientCvvs = new ConcurrentHashMap<>();

    private Stage stage;
    private Stage parentStage;

    public void setAppController(BuyProductController app) {
        this.appController = app;
        refreshView();
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
        if (dataObj instanceof Runnable callback) {
            this.onCartUpdated = callback;
        }
        refreshView();
    }

    private void refreshView() {
        if(appController == null) return;

        cards.setAll(appController.loadSavedCardsForCurrentUser().stream()
                .map(CardViewModel::new)
                .toList());

        NumberFormat fmt = NumberFormat.getCurrencyInstance(java.util.Locale.ITALY);
        BigDecimal total = appController.buildCheckoutData().total();
        if(totalLabel != null){
            totalLabel.setText(fmt.format(total));
        }

        cardsTable.refresh();
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
        if(appController == null){
            showError("Controller applicativo non disponibile.");
            return;
        }

        String address = (addressField != null) ? addressField.getText() : null;

        CardViewModel selectedVM = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedVM == null) {
            showInfo("Seleziona una carta salvata per procedere.");
            return;
        }

        Card selected = selectedVM.toDto();
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
                CheckoutRequest request = new CheckoutRequest(selected, cvv, address);
                CheckoutResult result = appController.confirmPayment(request);
                Platform.runLater(() -> {
                    setProcessing(false);
                    if (result.success()) {
                        onPaymentSuccess(result);
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

    private void onPaymentSuccess(CheckoutResult result) {
        transientCvvs.clear();
        appController.clearCart();

        if (parentStage != null) parentStage.close();
        if (onCartUpdated != null) onCartUpdated.run();

        Alert ok = new Alert(Alert.AlertType.INFORMATION);
        ok.setTitle("Ordine completato");
        ok.setHeaderText("ID Ordine: " + result.orderIds());
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

        var result = appController.addInlineCard(data);

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
