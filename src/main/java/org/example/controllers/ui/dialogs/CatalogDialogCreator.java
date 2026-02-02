package org.example.controllers.ui.dialogs;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.models.CatalogForm;
import org.example.dao.SellerDAO;
import org.example.controllers.app.SellerHomeAppController;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CatalogDialogCreator {
    protected final String title;
    protected final CatalogForm initial;
    protected final Button ownerButton;
    protected final SellerHomeAppController appController;

    private static final String TEXT_DARK = "#0f172a";
    private static final String TEXT_MUTED = "#64748b";
    private static final String BORDER = "#e5e7eb";
    private static final String ACCENT = "#d32f2f";
    private static final String SURFACE = "#ffffff";
    private static final String RADIUS_LG = "16";
    private static final String RADIUS_SM = "10";
    private static final String NEUTRAL_HOV = "#f8fafc";
    private static final String NEUTRAL_PRES = "#eef2f7";
    private static final String BORDER_HOV = "#d32f2f";
    private static final String ACCENT_HOV = "#b71c1c";
    private static final String ACCENT_PRES = "#8b1010";
    private static final String SHADOW = "dropshadow(gaussian, rgba(15,23,42,0.18), 22, 0.18, 0, 8)";
    private static final String PROP_SUPPRESS_TA_ONCE = "suppressTypeaheadOnce";

    private static final String BTN_STYLE_FMT = """
            -fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: %s; -fx-padding: 10 16; -fx-cursor: hand;
            """;

    private static final String BTN_SECONDARY_FMT = """
            -fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold;
            -fx-background-radius: %s; -fx-padding: 10 16; -fx-cursor: hand;
            -fx-border-color: %s; -fx-border-radius: %s; -fx-border-width: 2;
            """;

    protected CatalogDialogCreator(String title, CatalogForm initial, Button ownerButton, SellerHomeAppController appController) {
        this.title = title;
        this.initial = initial;
        this.ownerButton = ownerButton;
        this.appController = appController;
    }

    public final Dialog<CatalogForm> newDialog() {
        Dialog<CatalogForm> dialog = createBaseDialog(title);
        DialogPane pane = dialog.getDialogPane();

        String subtitle = (initial == null)
                ? "Cerca o seleziona il prodotto dalla tendina. La scelta avviene solo cliccando sulla lista."
                : "Stai modificando questo prodotto.";
        pane.setHeader(buildHeader(title, subtitle));

        ProductUI ui = createProductUI(initial);

        Styles styles = makeStyles();
        applyFieldStyles(ui, styles, initial == null);

        if (initial == null) {
            setupAddModeHandlers(ui.combo);
            appController.loadAllProducts(ui.combo,
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg));
        } else {
            prefillEditMode(initial, ui); // idem
        }

        pane.setContent(buildFormGrid(ui, initial != null));
        styleButtons(pane);

        attachValidationAndResult(dialog, ui, styles, initial);

        dialog.setOnShown(ev -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.sizeToScene();
        });
        Platform.runLater(() -> (initial != null ? ui.price : ui.combo.getEditor()).requestFocus());

        return dialog;
    }

    protected Dialog<CatalogForm> createBaseDialog(String title) {
        Dialog<CatalogForm> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setResizable(true);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        pane.setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: %s;
                -fx-padding: 18 18 16 18;
                -fx-effect: %s;
                """.formatted(SURFACE, RADIUS_LG, SHADOW));
        return dialog;
    }

    protected VBox buildHeader(String title, String subtitle) {
        Label headerTitle = new Label(title);
        headerTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + ACCENT + ";");

        Label headerSubtitle = new Label(subtitle);
        headerSubtitle.setStyle("-fx-font-size: 12; -fx-text-fill: " + TEXT_MUTED + ";");

        VBox headerBox = new VBox(headerTitle, headerSubtitle);
        headerBox.setSpacing(4);
        headerBox.setStyle("""
                -fx-padding: 0 0 12 0;
                -fx-border-color: transparent transparent %s transparent;
                -fx-border-width: 0 0 1 0;
                """.formatted(BORDER));
        return headerBox;
    }

    private ProductUI createProductUI(CatalogForm initial) {
        ComboBox<SellerDAO.ProductOption> cb = new ComboBox<>();
        cb.setEditable(true);
        cb.setPromptText("Cerca o seleziona prodotto");
        cb.setConverter(new StringConverter<>() {
            @Override public String toString(SellerDAO.ProductOption item) {
                return item == null ? "" : item.toString();
            }
            @Override public SellerDAO.ProductOption fromString(String s) {
                return cb.getValue();
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SellerDAO.ProductOption item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? cb.getPromptText() : item.toString());
            }
        });
        cb.setCellFactory(lv -> {
            ListCell<SellerDAO.ProductOption> cell = new ListCell<>() {
                @Override protected void updateItem(SellerDAO.ProductOption item, boolean empty) {
                    super.updateItem(item, empty);
                    setText((empty || item == null) ? null : item.toString());
                }
            };
            cell.setOnMouseClicked(me -> {
                if (!cell.isEmpty()) {
                    cb.setValue(cell.getItem());
                    cb.hide();
                }
            });
            return cell;
        });

        TextField tfProductName = new TextField();
        tfProductName.setEditable(false);
        tfProductName.setDisable(true);
        tfProductName.setPromptText("Prodotto");

        TextField tfSize = new TextField();
        tfSize.setPromptText("Taglia (es. 42, M, unique)");

        TextField tfPrice = new TextField();
        tfPrice.setPromptText("Prezzo (es. 99.90)");

        TextField tfQty = new TextField();
        tfQty.setPromptText("Quantità");

        return (initial == null)
                ? new ProductUI(cb, null, tfSize, tfPrice, tfQty)
                : new ProductUI(null, tfProductName, tfSize, tfPrice, tfQty);
    }

    protected Styles makeStyles() {
        String base = """
                -fx-background-radius: %s; -fx-border-radius: %s; -fx-padding: 10 12;
                -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1;
                -fx-text-fill: %s; -fx-prompt-text-fill: #94a3b8;
                """.formatted(RADIUS_SM, RADIUS_SM, SURFACE, BORDER, TEXT_DARK);
        String hover = base + "-fx-background-insets: 0; -fx-border-color: #d1d5db;";
        String focus = base + "-fx-effect: dropshadow(gaussian, rgba(211,47,47,0.18), 10, 0.2, 0, 2); -fx-border-color: " + ACCENT + ";";
        String error = base + "-fx-border-color: #ef4444; -fx-border-width: 1.5;";
        return new Styles(base, hover, focus, error);
    }

    protected void applyFieldStyles(ProductUI ui, Styles s, boolean isAdd) {
        List<Control> controls = isAdd
                ? List.of(ui.combo.getEditor(), ui.size, ui.price, ui.qty)
                : List.of(ui.name, ui.size, ui.price, ui.qty);

        for (Control c : controls) {
            c.setStyle(s.base);
            c.focusedProperty().addListener((obs, was, now) ->
                    c.setStyle(Boolean.TRUE.equals(now) ? s.focus : s.base));
            c.hoverProperty().addListener((obs, was, now) -> {
                if (!c.isFocused()) c.setStyle(Boolean.TRUE.equals(now) ? s.hover : s.base);
            });
        }
    }

    protected GridPane buildFormGrid(ProductUI ui, boolean isEdit) {
        GridPane gp = new GridPane();
        gp.setHgap(12);
        gp.setVgap(14);
        gp.setStyle("-fx-padding: 14 2 2 2;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        gp.getColumnConstraints().setAll(col1, col2);

        String labelStyle = "-fx-text-fill: " + TEXT_MUTED + "; -fx-font-weight: bold; -fx-font-size: 12;";
        Label lProd = new Label("Prodotto:");
        Label lSize = new Label("Taglia:");
        Label lPrice = new Label("Prezzo (€):");
        Label lQty = new Label("Quantità:");

        for (Label l : List.of(lProd, lSize, lPrice, lQty)) {
            l.setStyle(labelStyle);
            l.setEllipsisString("");
            l.setWrapText(true);
            l.setMinWidth(Region.USE_PREF_SIZE);
        }

        if (isEdit) gp.addRow(0, lProd, ui.name);
        else gp.addRow(0, lProd, ui.combo);

        gp.addRow(1, lSize, ui.size);
        gp.addRow(2, lPrice, ui.price);
        gp.addRow(3, lQty, ui.qty);

        if (isEdit) GridPane.setFillWidth(ui.name, true);
        else GridPane.setFillWidth(ui.combo, true);

        GridPane.setFillWidth(ui.size, true);
        GridPane.setFillWidth(ui.price, true);
        GridPane.setFillWidth(ui.qty, true);

        return gp;
    }

    protected void styleButtons(DialogPane pane) {
        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setDefaultButton(true);
        okBtn.setText("Salva");

        Button cancelBtn = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelBtn.setCancelButton(true);
        cancelBtn.setText("Annulla");

        applyOkBtnStyle(okBtn, false, false);
        applyCancelBtnStyle(cancelBtn, false, false);

        okBtn.hoverProperty().addListener((obs, was, now) ->
                applyOkBtnStyle(okBtn, now, okBtn.isPressed()));
        okBtn.pressedProperty().addListener((obs, was, now) ->
                applyOkBtnStyle(okBtn, okBtn.isHover(), now));

        cancelBtn.hoverProperty().addListener((obs, was, now) ->
                applyCancelBtnStyle(cancelBtn, now, cancelBtn.isPressed()));
        cancelBtn.pressedProperty().addListener((obs, was, now) ->
                applyCancelBtnStyle(cancelBtn, cancelBtn.isHover(), now));

        var bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setStyle("-fx-padding: 12 0 0 0;");
        }
    }

    protected void setupAddModeHandlers(ComboBox<SellerDAO.ProductOption> cb) {
        bindEditorToValue(cb);
        installShowAllOnOpen(cb);
        blockEnterCommit(cb);
        dropValueOnTyping(cb);
        fixPopupSpaceIssue(cb);
        attachTypeahead(cb);
        syncEditorOnBlur(cb);
    }

    private void prefillEditMode(CatalogForm initial, ProductUI ui) {
        ui.size.setText(initial.size());
        ui.size.setDisable(true);
        ui.price.setText(initial.price().toPlainString());
        ui.qty.setText(String.valueOf(initial.quantity()));
        appController.runAsync(
                SellerDAO::listAllProductOptions,
                opts -> opts.stream()
                        .filter(o -> o.productId() == initial.productId())
                        .findFirst()
                        .ifPresentOrElse(
                                o -> ui.name.setText(o.name()),
                                () -> ui.name.setText("Prodotto #" + initial.productId())
                        ),
                ex -> {
                    ui.name.setText("Prodotto #" + initial.productId());
                    showAlert(Alert.AlertType.ERROR, "Errore nel caricamento prodotto: " + ex.getMessage());
                }
        );
    }

    private void installShowAllOnOpen(ComboBox<SellerDAO.ProductOption> cb) {
        cb.showingProperty().addListener((obs, was, showing) -> {
            if (!Boolean.TRUE.equals(showing)) return;
            if (appController.normalizeQuery(cb.getEditor().getText()).isEmpty()) {
                appController.loadAllProducts(cb,
                        errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg));
            }
        });
    }

    private void applyOkBtnStyle(Button b, boolean hover, boolean pressed) {
        b.setStyle(BTN_STYLE_FMT.formatted(computeOkBtnColor(hover, pressed), RADIUS_SM));
    }

    private void applyCancelBtnStyle(Button b, boolean hover, boolean pressed) {
        b.setStyle(BTN_SECONDARY_FMT.formatted(
                computeCancelBtnBg(hover, pressed),
                ACCENT,
                RADIUS_SM,
                BORDER_HOV,
                RADIUS_SM
        ));
    }

    private String computeCancelBtnBg(boolean hover, boolean pressed) {
        if (pressed) return NEUTRAL_PRES;
        if (hover) return NEUTRAL_HOV;
        return SURFACE;
    }

    private String computeOkBtnColor(boolean hover, boolean pressed) {
        if (pressed) return ACCENT_PRES;
        if (hover) return ACCENT_HOV;
        return ACCENT;
    }

    private void blockEnterCommit(ComboBox<SellerDAO.ProductOption> cb) {
        cb.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) e.consume();
        });
        cb.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) e.consume();
        });
    }

    private void fixPopupSpaceIssue(ComboBox<SellerDAO.ProductOption> cb) {
        final AtomicBoolean suppressNext = new AtomicBoolean(false);

        cb.getEditor().addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (" ".equals(e.getCharacter()) && suppressNext.get()) {
                e.consume();
                suppressNext.set(false);
            }
        });

        cb.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (!(newSkin instanceof ComboBoxListViewSkin<?> skin)) return;
            Node popupContent = skin.getPopupContent();
            if (!(popupContent instanceof ListView<?> lv)) return;

            lv.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.SPACE && cb.getEditor().isFocused()) {
                    var ed = cb.getEditor();
                    ed.insertText(ed.getCaretPosition(), " ");
                    suppressNext.set(true);
                    e.consume();
                    return;
                }
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                }
            });
        });
    }

    private void attachTypeahead(ComboBox<SellerDAO.ProductOption> cb) {
        final PauseTransition debounce = new PauseTransition(Duration.millis(250));
        final String[] lastQ = { "" };

        cb.getEditor().textProperty().addListener((o, old, neu) -> {
            if (handleSelectionSuppressed(cb)) return;

            String q = appController.extractNameForSearch(neu);
            if (handleEmptyOrUnchangedQuery(cb, debounce, lastQ, q)) return;

            boolean wasShowing = cb.isShowing();
            String[] tokens = q.split(" ");
            String first = tokens[0];

            scheduleTypeaheadSearch(cb, debounce, tokens, first, q, lastQ, wasShowing);
        });
    }

    private boolean handleSelectionSuppressed(ComboBox<?> cb) {
        if (Boolean.TRUE.equals(cb.getProperties().get(PROP_SUPPRESS_TA_ONCE))) {
            cb.getProperties().put(PROP_SUPPRESS_TA_ONCE, Boolean.FALSE);
            return true;
        }
        return false;
    }

    private boolean handleEmptyOrUnchangedQuery(ComboBox<SellerDAO.ProductOption> cb,
                                                PauseTransition debounce,
                                                String[] lastQ,
                                                String q) {
        if (q.isEmpty()) {
            debounce.stop();
            lastQ[0] = "";
            if (cb.isShowing()) appController.loadAllProducts(cb,
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg));
            return true;
        }
        return q.equalsIgnoreCase(lastQ[0]);
    }

    private void scheduleTypeaheadSearch(ComboBox<SellerDAO.ProductOption> cb,
                                         PauseTransition debounce,
                                         String[] tokens,
                                         String first,
                                         String q,
                                         String[] lastQ,
                                         boolean wasShowing) {
        debounce.stop();
        debounce.setOnFinished(evt -> appController.runAsync(
                () -> SellerDAO.listProductOptionsByNameLike(first, 100),
                opts -> {
                    var filtered = opts.stream().filter(o2 -> appController.matchesAllTokens(o2, tokens)).toList();
                    if (!filtered.isEmpty()) {
                        cb.getItems().setAll(filtered);
                        if (wasShowing) cb.show();
                    } else {
                        cb.getItems().clear();
                        cb.hide();
                    }
                    lastQ[0] = q;
                },
                ex -> showAlert(Alert.AlertType.ERROR, "Errore ricerca prodotti: " + ex.getMessage())
        ));
        debounce.playFromStart();
    }

    private void bindEditorToValue(ComboBox<SellerDAO.ProductOption> cb) {
        cb.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            cb.getProperties().put(PROP_SUPPRESS_TA_ONCE, Boolean.TRUE);
            Platform.runLater(() -> cb.getEditor().setText(cb.getConverter().toString(newV)));
        });
    }

    private void syncEditorOnBlur(ComboBox<SellerDAO.ProductOption> cb) {
        cb.focusedProperty().addListener((obs, was, now) -> {
            if (Boolean.TRUE.equals(now)) return;
            var val = cb.getValue();
            if (val != null) cb.getEditor().setText(cb.getConverter().toString(val));
            else cb.getEditor().clear();
        });
    }

    private void dropValueOnTyping(ComboBox<SellerDAO.ProductOption> cb) {
        cb.getEditor().addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (Boolean.TRUE.equals(cb.getProperties().get(PROP_SUPPRESS_TA_ONCE))) return;
            if (cb.getValue() != null) cb.setValue(null);
        });
        cb.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case BACK_SPACE, DELETE -> {
                    if (Boolean.TRUE.equals(cb.getProperties().get(PROP_SUPPRESS_TA_ONCE))) return;
                    if (cb.getValue() != null) cb.setValue(null);
                }
                default -> {
                    // Il comportamento è gestito altrove
                }
            }
        });
    }

    public void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        if (ownerButton != null && ownerButton.getScene() != null) {
            a.initOwner(ownerButton.getScene().getWindow());  // Parent dalla finestra chiamante
        }
        a.showAndWait();
    }

    protected abstract void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial);

    protected CatalogForm extractForm(ProductUI ui, int productId) {
        try {
            String size = ui.size().getText().trim();
            BigDecimal price = new BigDecimal(ui.price().getText().trim());
            int qty = Integer.parseInt(ui.qty().getText().trim());
            CatalogForm form = new CatalogForm(productId, size, price, qty);
            if (appController.isValidCatalogForm(form)) return form;
            showAlert(Alert.AlertType.WARNING, "Prezzo o quantità non validi");
            return null;
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Errore form: " + ex.getMessage());
            return null;
        }
    }


    protected record ProductUI(ComboBox<SellerDAO.ProductOption> combo,
                               TextField name,
                               TextField size,
                               TextField price,
                               TextField qty) {
    }

    protected record Styles(String base, String hover, String focus, String error) {
    }
}