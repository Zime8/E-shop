package org.example.controllers.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.controllers.app.SavedCardsAppController;
import org.example.controllers.control.SavedCardsControl;
import org.example.models.Card;

import java.util.Optional;

public class SavedCardsController {

    @FXML private ListView<Card> cardsListView;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final SavedCardsControl appController = new SavedCardsAppController();

    // Costanti UI
    private static final String CARD_TYPE_CREDITO = "Credito";
    private static final String CARD_TYPE_DEBITO  = "Debito";
    private static final String STYLE_TEXT_DARK = "-fx-text-fill: #444;";
    private static final String STYLE_OPACITY_HOVER = "-fx-opacity: 0.85;";

    private static final String CANCEL_BTN_STYLE =
            "-fx-background-color: white;" +
                    "-fx-border-color: #d32f2f;" +
                    "-fx-border-width: 2;" +
                    "-fx-text-fill: #d32f2f;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 15;" +
                    "-fx-border-radius: 15;";

    private static final String CARD_STYLE_BASE =
            "-fx-padding: 12 14;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #d32f2f;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 14;" +
                    "-fx-effect: dropshadow(gaussian, rgba(211,47,47,0.10), 12, 0.18, 0, 4);";

    private static final String CARD_STYLE_SELECTED =
            "-fx-padding: 12 14;" +
                    "-fx-background-color: rgba(211,47,47,0.06);" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #d32f2f;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 14;" +
                    "-fx-effect: dropshadow(gaussian, rgba(211,47,47,0.22), 18, 0.28, 0, 7);";

    private static final String CARD_STYLE_HOVER =
            "-fx-padding: 12 14;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #d32f2f;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 14;" +
                    "-fx-effect: dropshadow(gaussian, rgba(211,47,47,0.20), 16, 0.25, 0, 6);";

    @FXML
    public void initialize() {

        cardsListView.setItems(cards);
        cardsListView.setPlaceholder(new Label("Nessuna carta salvata"));
        cardsListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background-insets: 0;");
        cardsListView.setCellFactory(listView -> new CardCell());

        reloadCards();  // Carica dati iniziali
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) cardsListView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAddCard() {
        Optional<Card> result = showCardDialog("Aggiungi Carta", "Inserisci i dati della nuova carta", "Aggiungi", null);
        result.ifPresent(card -> {
            try {
                Card newCard = appController.addCard(card.holder(), card.number(), card.expiry(), card.type());

                reloadCards();
                cardsListView.scrollTo(newCard);
                showInfo("Carta aggiunta!");

            } catch (IllegalStateException e) {
                showError("Carta già salvata: " + SavedCardsAppController.maskPan(card.number()));
            } catch (Exception e) {
                showError("Errore salvataggio: " + e.getMessage());
            }
        });
    }


    private void reloadCards() {
        try {
            cards.setAll(appController.loadCards());
            cardsListView.refresh();
        } catch (RuntimeException e) {
            showError("Errore caricamento: " + e.getMessage());
            cards.clear();
        }
    }

    // === METODI UI NOTIFICHE ===

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText("Attenzione");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText("Operazione riuscita");
        alert.setContentText(content);
        alert.showAndWait();
    }

    // === DIALOG CARDS ===

    private Optional<Card> showCardDialog(String title, String headerText, String okText, Card prefill) {
        Dialog<Card> dialog = new Dialog<>();
        dialog.setTitle(title);

        Label header = new Label(headerText);
        header.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 16px; -fx-font-weight: bold;");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setHeader(header);

        ButtonType okType = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(CANCEL_BTN_STYLE);

        FormControls fc = buildForm(prefill);
        dialog.getDialogPane().setContent(fc.grid());

        var cssUrl = getClass().getResource("/css/cards.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        dialog.setResultConverter(btn -> {
            if (btn != okType) return null;

            String holder = fc.holder().getText().trim();
            String number = fc.number().getText();
            String expiry = fc.expiry().getText().trim();
            String type = fc.type().getValue();

            if (prefill == null) {
                return new Card(0, holder, number, expiry, type);
            } else {
                return new Card(prefill.id(), holder, number, expiry, type);
            }
        });

        return dialog.showAndWait();
    }

    private record FormControls(TextField holder, TextField number, TextField expiry, ChoiceBox<String> type, GridPane grid) {}

    private FormControls buildForm(Card prefill) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);

        TextField holderField = new TextField(prefill == null ? "" : prefill.holder());
        holderField.setPromptText("Intestatario");
        TextField numberField = new TextField(prefill == null ? "" : prefill.number());
        numberField.setPromptText("Numero carta");
        TextField expiryField = new TextField(prefill == null ? "" : prefill.expiry());
        expiryField.setPromptText("Scadenza (MM/AA)");

        ChoiceBox<String> typeChoice = new ChoiceBox<>();
        typeChoice.getItems().addAll(CARD_TYPE_CREDITO, CARD_TYPE_DEBITO);
        typeChoice.setValue(prefill == null ? CARD_TYPE_CREDITO : prefill.type());

        grid.add(new Label("Intestatario:"), 0, 0);
        grid.add(holderField, 1, 0);
        grid.add(new Label("Numero carta:"), 0, 1);
        grid.add(numberField, 1, 1);
        grid.add(new Label("Scadenza:"), 0, 2);
        grid.add(expiryField, 1, 2);
        grid.add(new Label("Tipo:"), 0, 3);
        grid.add(typeChoice, 1, 3);

        return new FormControls(holderField, numberField, expiryField, typeChoice, grid);
    }

    // === CardCell INTERNA ===

    private class CardCell extends ListCell<Card> {
        private final Label holderLabel = new Label();
        private final Label numberLabel = new Label();
        private final Label expiryLabel = new Label();
        private final Label typeLabel = new Label();
        private final Button editBtn = new Button("Modifica");
        private final Button delBtn = new Button("Elimina");
        private final HBox cardRoot = new HBox(14);

        public CardCell() {
            holderLabel.setStyle(STYLE_TEXT_DARK);
            numberLabel.setStyle(STYLE_TEXT_DARK);
            expiryLabel.setStyle(STYLE_TEXT_DARK);
            typeLabel.setStyle(STYLE_TEXT_DARK);

            VBox leftBox = new VBox(6);
            leftBox.getChildren().addAll(holderLabel, numberLabel, expiryLabel, typeLabel);

            editBtn.setMaxWidth(Double.MAX_VALUE);
            delBtn.setMaxWidth(Double.MAX_VALUE);

            String btnStyle = "-fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-font-size: 13; -fx-font-weight: bold; -fx-underline: true;";
            editBtn.setStyle(btnStyle);
            delBtn.setStyle(btnStyle);

            editBtn.setOnMouseEntered(e -> editBtn.setStyle(btnStyle + STYLE_OPACITY_HOVER));
            editBtn.setOnMouseExited(e -> editBtn.setStyle(btnStyle));
            delBtn.setOnMouseEntered(e -> delBtn.setStyle(btnStyle + STYLE_OPACITY_HOVER));
            delBtn.setOnMouseExited(e -> delBtn.setStyle(btnStyle));

            VBox rightBox = new VBox(8);
            rightBox.getChildren().addAll(editBtn, delBtn);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Region redStripe = new Region();
            redStripe.setPrefWidth(4);
            redStripe.setMinWidth(4);
            redStripe.setMaxWidth(4);
            redStripe.setStyle("-fx-background-color: #d32f2f; -fx-background-radius: 4;");

            cardRoot.getChildren().addAll(redStripe, leftBox, spacer, rightBox);
            cardRoot.setAlignment(Pos.CENTER_LEFT);
            cardRoot.setStyle(CARD_STYLE_BASE);

            cardRoot.setOnMouseEntered(e -> {
                if (!isEmpty() && !isSelected()) cardRoot.setStyle(CARD_STYLE_HOVER);
            });
            cardRoot.setOnMouseExited(e -> applyCardStyle(isSelected()));

            selectedProperty().addListener((obs, wasSel, isSel) -> applyCardStyle(isSel));
        }

        private void applyCardStyle(boolean selected) {
            cardRoot.setStyle(selected ? CARD_STYLE_SELECTED : CARD_STYLE_BASE);
        }

        @Override
        protected void updateItem(Card card, boolean empty) {
            super.updateItem(card, empty);
            if (empty || card == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            holderLabel.setText("Intestatario: " + card.holder());
            numberLabel.setText("Numero: " + SavedCardsAppController.maskPan(card.number()));  // Statico!
            expiryLabel.setText("Scadenza: " + card.expiry());
            typeLabel.setText("Tipo: " + card.type());

            // Bottoni chiamano METODI del parent controller UI
            editBtn.setOnAction(e -> onEditCard(card));
            delBtn.setOnAction(e -> onDeleteCard(card));

            applyCardStyle(isSelected());
            setGraphic(cardRoot);
            setText(null);
            setStyle("-fx-background-color: transparent;");
        }
    }

    // === EVENT HANDLER per CardCell ===

    private void onEditCard(Card card) {
        Optional<Card> result = showCardDialog("Modifica Carta", "Aggiorna i dati della carta", "Salva", card);
        result.ifPresent(updated -> {
            try {
                appController.editCard(card.id(), updated.holder(), updated.number(), updated.expiry(), updated.type());
                reloadCards();
                showInfo("Carta aggiornata!");
            } catch (IllegalStateException e) {
                showError(e.getMessage());
            } catch (IllegalArgumentException ex){
                showError("Controller luhn non superato.");
            }
        });
    }

    private void onDeleteCard(Card card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText("Eliminare questa carta?");
        confirm.setContentText("Intestatario: " + card.holder() + "\nNumero: " + SavedCardsAppController.maskPan(card.number()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                if (appController.deleteCard(card.id())) {  // Solo int ID
                    reloadCards();
                    showInfo("Carta eliminata!");
                } else {
                    showError("Carta non trovata");
                }
            } catch (RuntimeException e) {
                showError("Errore eliminazione: " + e.getMessage());
            }
        }
    }
}
