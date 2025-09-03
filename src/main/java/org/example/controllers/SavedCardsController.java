package org.example.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.dao.SavedCardsDAO;
import org.example.models.Card;
import org.example.util.Session;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SavedCardsController {

    @FXML private ListView<Card> cardsListView;

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final SavedCardsDAO cardsDao = new SavedCardsDAO();

    private static final String CARD_TYPE_CREDITO = "Credito";
    private static final String CARD_TYPE_DEBITO  = "Debito";
    private static final String ERR_DB_HEADER = "Errore database";
    private static final String STYLE_TEXT_DARK = "-fx-text-fill: #444;";
    private static final String STYLE_OPACITY_HOVER = "-fx-opacity: 0.85;";

    // Stili centralizzati
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

    private static final Logger logger = Logger.getLogger(SavedCardsController.class.getName());

    @FXML
    public void initialize() {
        cardsListView.setItems(cards);
        cardsListView.setPlaceholder(new Label("Nessuna carta salvata"));
        cardsListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-background-insets: 0;");
        cardsListView.setCellFactory(listView -> new CardCell());
        reloadFromDb();
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) cardsListView.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAddCard() {
        Optional<Card> res = promptCard(
                "Aggiungi Carta",
                "Inserisci i dati della nuova carta",
                "Aggiungi",
                null
        );
        if (res.isEmpty()) return;

        Card input = res.get();
        try {
            String holder = validateHolder(input.getHolder());
            String rawPan = input.getNumber();
            String normPan = normalizePan(rawPan);
            validatePan(normPan);
            String expiry = validateExpiry(input.getExpiry());
            String type = validateType(input.getType());

            Optional<Integer> insertedId = cardsDao.insertIfAbsentReturningId(Session.getUserId(), holder, rawPan, expiry, type);
            if (insertedId.isEmpty()) {
                showError("Carta già presente", "Questa carta risulta già salvata per il tuo account.");
                return;
            }

            cards.add(new Card(insertedId.get(), holder, rawPan, expiry, type));
            showInfo();

        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Dati non validi in inserimento", e);
            showAlert("Dati non validi");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, ERR_DB_HEADER, e);
            showAlert(ERR_DB_HEADER);
        }
    }

    private void reloadFromDb() {
        cards.clear();
        try {
            for (SavedCardsDAO.Row r : cardsDao.findByUser(Session.getUserId())) {
                cards.add(new Card(r.getId(), r.getHolder(), r.getCardNumber(), r.getExpiry(), r.getCardType()));
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Errore caricamento carte", ex);
            showAlert("Errore caricamento carte");
        }
    }

    /* =======================
       Dialog building (no duplicati)
       ======================= */

    private record FormControls(TextField holder, TextField number, TextField expiry, ChoiceBox<String> type, GridPane grid) {}

    private Optional<Card> promptCard(String title, String headerText, String okText, Card prefill) {
        Dialog<Card> dialog = new Dialog<>();
        dialog.setTitle(title);

        Label header = new Label(headerText);
        header.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setHeader(header);

        ButtonType okType = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        // stile Cancel coerente
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(CANCEL_BTN_STYLE);

        FormControls fc = buildForm(prefill);
        dialog.getDialogPane().setContent(fc.grid());

        // CSS opzionale (se lo usi)
        var cssUrl = getClass().getResource("/css/cards.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        dialog.setResultConverter(btn -> {
            if (btn != okType) return null;

            String holder = fc.holder().getText();
            String number = fc.number().getText();
            String expiry = fc.expiry().getText();
            String type   = fc.type().getValue();

            if (prefill == null) {
                return new Card(holder, number, expiry, type);
            } else {
                return new Card(prefill.getId(), holder, number, expiry, type);
            }
        });

        return dialog.showAndWait();
    }

    private FormControls buildForm(Card prefill) {
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);

        TextField holderField = new TextField(prefill == null ? "" : prefill.getHolder()); holderField.setPromptText("Intestatario");
        TextField numberField = new TextField(prefill == null ? "" : prefill.getNumber()); numberField.setPromptText("Numero carta");
        TextField expiryField = new TextField(prefill == null ? "" : prefill.getExpiry()); expiryField.setPromptText("Scadenza (MM/AA)");

        ChoiceBox<String> typeChoice = new ChoiceBox<>();
        typeChoice.getItems().addAll(CARD_TYPE_CREDITO, CARD_TYPE_DEBITO);
        typeChoice.setValue(prefill == null ? CARD_TYPE_CREDITO : prefill.getType());

        grid.add(new Label("Intestatario:"), 0, 0); grid.add(holderField, 1, 0);
        grid.add(new Label("Numero carta:"), 0, 1); grid.add(numberField, 1, 1);
        grid.add(new Label("Scadenza:"), 0, 2); grid.add(expiryField, 1, 2);
        grid.add(new Label("Tipo:"), 0, 3); grid.add(typeChoice, 1, 3);

        return new FormControls(holderField, numberField, expiryField, typeChoice, grid);
    }

    /* =======================
       VALIDAZIONI & UTILS
       ======================= */

    private static String validateHolder(String holder) {
        if (holder == null || holder.trim().isEmpty())
            throw new IllegalArgumentException("L'intestatario non può essere vuoto.");
        return holder.trim();
    }

    private static String normalizePan(String pan) {
        return pan == null ? "" : pan.replaceAll("\\D", "");
    }

    private static void validatePan(String digits) {
        if (digits.length() < 13 || digits.length() > 19)
            throw new IllegalArgumentException("Il numero carta deve avere tra 13 e 19 cifre.");
        if (!luhnOk(digits))
            throw new IllegalArgumentException("Il numero carta non supera il controllo Luhn.");
    }

    private static boolean luhnOk(String d) {
        int sum = 0;
        boolean alt = false;

        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alt) { n *= 2; if (n > 9) n -= 9; }
            sum += n; alt = !alt;
        }

        return sum % 10 == 0;
    }

    private static String validateExpiry(String expiry) {
        if (expiry == null) throw new IllegalArgumentException("Inserisci la scadenza (MM/AA).");
        String e = expiry.trim();
        if (!e.matches("^(0[1-9]|1[0-2])/\\d{2}$"))
            throw new IllegalArgumentException("Formato scadenza non valido. Usa MM/AA (es. 12/27).");
        int month = Integer.parseInt(e.substring(0, 2));
        int year  = 2000 + Integer.parseInt(e.substring(3, 5));
        var expYm = YearMonth.of(year, month);
        var nowYm = YearMonth.now();
        if (expYm.isBefore(nowYm))
            throw new IllegalArgumentException("La carta risulta scaduta.");
        return e;
    }

    private static String validateType(String type) {
        if (CARD_TYPE_CREDITO.equals(type) || CARD_TYPE_DEBITO.equals(type)) return type;
        throw new IllegalArgumentException("Tipo carta non valido. Usa 'Credito' o 'Debito'.");
    }

    private static String maskPan(String raw) {
        if (raw == null) return "";
        String d = raw.replaceAll("\\D", "");
        if (d.length() <= 4) return d;
        String last4 = d.substring(d.length() - 4);
        return "**** **** **** " + last4;
    }

    /* =======================
       CELL FACTORY
       ======================= */

    private class CardCell extends ListCell<Card> {
        private final Label holder = new Label();
        private final Label number = new Label();
        private final Label expiry = new Label();
        private final Label type   = new Label();

        private final Button editBtn = new Button("Modifica");
        private final Button delBtn  = new Button("Elimina");

        private final HBox cardRoot = new HBox(14);

        private CardCell() {
            holder.setStyle(STYLE_TEXT_DARK);
            number.setStyle(STYLE_TEXT_DARK);
            expiry.setStyle(STYLE_TEXT_DARK);
            type.setStyle(STYLE_TEXT_DARK);

            VBox leftBox = new VBox(6);
            leftBox.getChildren().addAll(holder, number, expiry, type);

            editBtn.setMaxWidth(Double.MAX_VALUE);
            delBtn.setMaxWidth(Double.MAX_VALUE);

            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-font-size: 13; -fx-font-weight: bold; -fx-underline: true;");
            delBtn.setStyle ("-fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-font-size: 13; -fx-font-weight: bold; -fx-underline: true;");
            editBtn.setOnMouseEntered(e -> editBtn.setStyle(editBtn.getStyle() + STYLE_OPACITY_HOVER));
            editBtn.setOnMouseExited (e -> editBtn.setStyle(editBtn.getStyle().replace(STYLE_OPACITY_HOVER,"")));
            delBtn.setOnMouseEntered (e -> delBtn.setStyle(delBtn.getStyle() + STYLE_OPACITY_HOVER));
            delBtn.setOnMouseExited  (e -> delBtn.setStyle(delBtn.getStyle().replace(STYLE_OPACITY_HOVER,"")));

            VBox rightBox = new VBox(8);
            rightBox.getChildren().addAll(editBtn, delBtn);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Region redStripe = new Region();
            redStripe.setPrefWidth(4); redStripe.setMinWidth(4); redStripe.setMaxWidth(4);
            redStripe.setStyle("-fx-background-color: #d32f2f; -fx-background-radius: 4;");

            cardRoot.getChildren().addAll(redStripe, leftBox, spacer, rightBox);
            cardRoot.setAlignment(Pos.CENTER_LEFT);
            cardRoot.setStyle(CARD_STYLE_BASE);

            cardRoot.setOnMouseEntered(e -> { if (!isEmpty() && !isSelected()) cardRoot.setStyle(CARD_STYLE_HOVER); });
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

            holder.setText("Intestatario: " + card.getHolder());
            number.setText("Numero: " + maskPan(card.getNumber()));
            expiry.setText("Scadenza: " + card.getExpiry());
            type.setText("Tipo: " + card.getType());

            editBtn.setOnAction(e -> onEditCard(card));
            delBtn.setOnAction(e -> onDeleteCard(card));

            applyCardStyle(isSelected());
            setGraphic(cardRoot);
            setText(null);
            setStyle("-fx-background-color: transparent;");
        }

        private void onEditCard(Card card) {
            Optional<Card> res = promptCard(
                    "Modifica Carta",
                    "Aggiorna i dati della carta",
                    "Salva",
                    card
            );
            if (res.isEmpty()) return;

            Card updated = res.get();
            try {
                String validateHolder = validateHolder(updated.getHolder());
                String rawPan = updated.getNumber();
                String normPan = normalizePan(rawPan);
                validatePan(normPan);
                String validateExpiry = validateExpiry(updated.getExpiry());
                String validateType = validateType(updated.getType());

                boolean ok = cardsDao.updateCard(card.getId(), Session.getUserId(), validateHolder, rawPan, validateExpiry, validateType);
                if (!ok) {
                    showError("Carta già presente", "Una carta con questo numero è già salvata per il tuo account.");
                    return;
                }

                card.setHolder(validateHolder);
                card.setNumber(rawPan);
                card.setExpiry(validateExpiry);
                card.setType(validateType);

                cardsListView.refresh();

                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Info");
                a.setHeaderText("Carta aggiornata");
                a.setContentText("Le modifiche sono state salvate.");
                a.showAndWait();

            } catch (IllegalArgumentException e) {
                logger.log(Level.SEVERE, "Dati non validi in modifica", e);
                showAlert("Dati non validi");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, ERR_DB_HEADER, e);
                showAlert(ERR_DB_HEADER);
            }
        }

        private void onDeleteCard(Card card) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Conferma eliminazione");
            confirm.setHeaderText("Eliminare questa carta?");
            confirm.setContentText("Intestatario: " + card.getHolder() + "\nNumero: " + maskPan(card.getNumber()));

            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.OK) return;

            try {
                boolean deleted = cardsDao.deleteById(card.getId(), Session.getUserId());
                if (!deleted) {
                    showError("Errore", "Carta non trovata o già rimossa.");
                    return;
                }
                cards.remove(card);

                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Info");
                a.setHeaderText("Carta eliminata");
                a.setContentText("La carta è stata rimossa.");
                a.showAndWait();

            } catch (SQLException e) {
                logger.log(Level.SEVERE, ERR_DB_HEADER, e);
                showAlert(ERR_DB_HEADER);
            }
        }
    }

    /* =======================
       Alert helpers
       ======================= */
    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Errore"); a.setHeaderText(header); a.setContentText(msg);
        a.showAndWait();
    }
    private void showInfo() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText("Carta aggiunta"); a.setContentText("La carta è stata salvata correttamente.");
        a.showAndWait();
    }
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
