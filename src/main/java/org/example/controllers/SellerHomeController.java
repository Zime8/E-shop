package org.example.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.dao.SellerDAO;
import org.example.dao.ShopDAO;
import org.example.util.Session;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.collections.FXCollections.observableArrayList;

public class SellerHomeController {

    private static final Logger logger = Logger.getLogger(SellerHomeController.class.getName());
    private static final List<String> ORDER_STATES = List.of("in elaborazione", "spedito", "consegnato", "annullato");
    private static final String PROP_SUPPRESS_TA_ONCE = "suppressTypeaheadOnce";

    // CSS / Theme
    private static final String ALIGN_CENTER = "-fx-alignment: CENTER;";
    private static final String ACCENT = "#d32f2f";
    private static final String ACCENT_HOV = "#b71c1c";
    private static final String ACCENT_PRES = "#8b1010";
    private static final String TEXT_DARK = "#0f172a";
    private static final String TEXT_MUTED = "#64748b";
    private static final String SURFACE = "#ffffff";
    private static final String BORDER = "#e5e7eb";
    private static final String SHADOW = "dropshadow(gaussian, rgba(15,23,42,0.18), 22, 0.18, 0, 8)";
    private static final String RADIUS_LG = "16";
    private static final String RADIUS_SM = "10";
    private static final String NEUTRAL_HOV = "#f8fafc";
    private static final String NEUTRAL_PRES = "#eef2f7";
    private static final String BORDER_HOV = "#d32f2f";

    private static final String BTN_SECONDARY_FMT = """
        -fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold;
        -fx-background-radius: %s; -fx-padding: 10 16; -fx-cursor: hand;
        -fx-border-color: %s; -fx-border-radius: %s; -fx-border-width: 2;
        """;

    // Prezzi/valute
    private static final NumberFormat CURR_IT = NumberFormat.getCurrencyInstance(Locale.ITALY);

    private boolean updatingFilters = false;

    // Esecutore async
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-ui-worker");
        t.setDaemon(true);
        return t;
    });

    // Header
    @FXML private Label shopNameLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane tabPane;
    @FXML private Label balanceLabel;
    @FXML private Button withdrawButton;
    private static final NumberFormat currencyIt = NumberFormat.getCurrencyInstance(Locale.ITALY);

    // Catalogo
    @FXML private TextField productSearchField;
    @FXML private ComboBox<String> brandFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TableView<SellerDAO.CatalogRow> productsTable;
    @FXML private TableColumn<SellerDAO.CatalogRow, Number> colProdId;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colProdName;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colSport;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colBrand;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colCategory;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colSize;
    @FXML private TableColumn<SellerDAO.CatalogRow, String> colPrice;
    @FXML private TableColumn<SellerDAO.CatalogRow, Number> colQuantity;

    // Ordini
    @FXML private ComboBox<String> orderStateFilter;
    @FXML private TableView<SellerDAO.ShopOrderSummary> sellerOrdersTable;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, Number> colOrderIdS;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, String> colOrderDateS;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, String> colOrderStateS;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, String> colOrderTotalS;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, String> colCustomerS;
    @FXML private TableColumn<SellerDAO.ShopOrderSummary, String> colAddress;

    @FXML private TableView<SellerDAO.ShopOrderLine> orderItemsTable;
    @FXML private TableColumn<SellerDAO.ShopOrderLine, String> colItemNameS;
    @FXML private TableColumn<SellerDAO.ShopOrderLine, String> colItemSizeS;
    @FXML private TableColumn<SellerDAO.ShopOrderLine, String> colItemPriceS;
    @FXML private TableColumn<SellerDAO.ShopOrderLine, String> colItemSubtotalS;
    @FXML private TableColumn<SellerDAO.ShopOrderLine, Number> colItemQtyS;

    @FXML private ComboBox<String> orderStateCombo;

    private Integer currentShopId;

    private final DateTimeFormatter dateFmt =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    @FXML
    private void initialize() {
        if (!ensureUserLoggedIn()) return;
        if (!loadSellerShop()) return;

        wireCatalogColumns();
        wireOrdersColumns();
        populateOrderStates();
        refreshBalance();

        setupPromptCombo(brandFilter, "Marca");
        setupPromptCombo(categoryFilter, "Categoria");

        setupPlaceholders();
        wireFilterListeners();
        wireOrderSelection();

        bootstrapData();
        installTableFixes();
    }

    /* ===================== Bind colonne ===================== */

    private void wireCatalogColumns() {
        colProdId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().productId()));
        colProdName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colSport.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().sport()));
        colBrand.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().brand()));
        colCategory.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().category()));
        colSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().size()));
        colPrice.setCellValueFactory(cd -> {
            BigDecimal p = cd.getValue().price();
            String s = (p == null) ? CURR_IT.format(0) : CURR_IT.format(p);
            return new SimpleStringProperty(s);
        });
        colQuantity.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().quantity()));

        colProdId.setStyle(ALIGN_CENTER);
        colProdName.setStyle(ALIGN_CENTER);
        colSport.setStyle(ALIGN_CENTER);
        colBrand.setStyle(ALIGN_CENTER);
        colCategory.setStyle(ALIGN_CENTER);
        colSize.setStyle(ALIGN_CENTER);
        colPrice.setStyle(ALIGN_CENTER);
        colQuantity.setStyle(ALIGN_CENTER);
    }

    private void wireOrdersColumns() {
        colOrderIdS.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().orderId()));
        colOrderDateS.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().orderDate() == null ? "" : dateFmt.format(cd.getValue().orderDate().toInstant())
        ));
        colOrderStateS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().state()));
        colOrderTotalS.setCellValueFactory(cd -> {
            BigDecimal t = cd.getValue().total();
            String s = (t == null) ? CURR_IT.format(0) : CURR_IT.format(t);
            return new SimpleStringProperty(s);
        });
        colCustomerS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().customer()));
        colAddress.setCellValueFactory(cd -> {
            String v = cd.getValue().address();
            return new SimpleStringProperty((v == null || v.isBlank()) ? "-" : v);
        });

        colItemNameS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().productName()));
        colItemSizeS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().size()));
        colItemQtyS.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().quantity()));
        colItemPriceS.setCellValueFactory(cd -> {
            BigDecimal up = cd.getValue().unitPrice();
            String s = (up == null) ? CURR_IT.format(0) : CURR_IT.format(up);
            return new SimpleStringProperty(s);
        });
        colItemSubtotalS.setCellValueFactory(cd -> new SimpleStringProperty(
                CURR_IT.format(cd.getValue().subtotal())
        ));

        colOrderIdS.setStyle(ALIGN_CENTER);
        colOrderDateS.setStyle(ALIGN_CENTER);
        colOrderStateS.setStyle(ALIGN_CENTER);
        colOrderTotalS.setStyle(ALIGN_CENTER);
        colCustomerS.setStyle(ALIGN_CENTER);
        colAddress.setStyle(ALIGN_CENTER);

        colItemNameS.setStyle(ALIGN_CENTER);
        colItemSizeS.setStyle(ALIGN_CENTER);
        colItemQtyS.setStyle(ALIGN_CENTER);
        colItemPriceS.setStyle(ALIGN_CENTER);
        colItemSubtotalS.setStyle(ALIGN_CENTER);
    }

    /* ========================= Catalogo: handler ========================= */

    @FXML
    private void onSearchProduct() {
        reloadCatalog();
    }

    @FXML
    private void onResetProductSearch() {
        if (productSearchField != null) productSearchField.clear();

        updatingFilters = true;
        try {
            if (brandFilter != null) {
                brandFilter.getSelectionModel().clearSelection();
                brandFilter.setValue(null);
            }
            if (categoryFilter != null) {
                categoryFilter.getSelectionModel().clearSelection();
                categoryFilter.setValue(null);
            }
        } finally {
            updatingFilters = false;
        }

        reloadCatalog();
    }

    @FXML
    private void onAddProduct() {
        var dialog = buildCatalogDialog("Aggiungi Prodotto", null);
        dialog.showAndWait().ifPresent(data -> runAsync(
                () -> {
                    SellerDAO.upsertCatalogRow(currentShopId, data.productId, data.size, data.price, data.quantity);
                    return null;
                },
                this::reloadCatalog,
                e -> showAlert(Alert.AlertType.ERROR, "Errore durante l'inserimento: " + e.getMessage())
        ));
    }

    @FXML
    private void onEditProduct() {
        var sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "Seleziona una riga da modificare.");
            return;
        }
        var dialog = buildCatalogDialog(
                "Modifica Prodotto",
                new CatalogForm(sel.productId(), sel.size(), nonNull(sel.price()), sel.quantity())
        );
        dialog.showAndWait().ifPresent(data -> runAsync(
                () -> {
                    SellerDAO.updateCatalogRow(currentShopId, sel.productId(), sel.size(), data.price, data.quantity);
                    return null;
                },
                this::reloadCatalog,
                e -> showAlert(Alert.AlertType.ERROR, "Errore durante l'aggiornamento: " + e.getMessage())
        ));
    }

    @FXML
    private void onDeleteProduct() {
        var sel = productsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "Seleziona una riga da rimuovere.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Confermi la rimozione dal catalogo?");
        confirm.setContentText(sel.name() + " - taglia " + sel.size());
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                runAsync(
                        () -> {
                            SellerDAO.deleteCatalogRow(currentShopId, sel.productId(), sel.size());
                            return null;
                        },
                        this::reloadCatalog,
                        e -> showAlert(Alert.AlertType.ERROR, "Errore durante la rimozione: " + e.getMessage())
                );
            }
        });
    }

    /* ========================= Ordini: handler ========================= */

    @FXML
    private void onApplyOrderFilters() {
        reloadOrders(orderStateFilter.getValue());
    }

    @FXML
    private void onResetOrderFilters() {
        orderStateFilter.getSelectionModel().clearSelection();
        reloadOrders(null);
    }

    @FXML
    private void onUpdateOrderStatus() {
        var sel = sellerOrdersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "Seleziona un ordine.");
            return;
        }
        String st = orderStateCombo.getValue();
        if (st == null || st.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Seleziona uno stato.");
            return;
        }
        runAsync(
                () -> {
                    SellerDAO.updateOrderState(sel.orderId(), st);
                    return null;
                },
                () -> {
                    reloadOrders(orderStateFilter.getValue());
                    Platform.runLater(() -> sellerOrdersTable.getSelectionModel().selectFirst());
                },
                e -> showAlert(Alert.AlertType.ERROR, "Errore durante l'aggiornamento dello stato: " + e.getMessage())
        );
    }

    /* ========================= Logout ========================= */

    @FXML
    private void onLogout() {
        Session.clear();
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.setMaximized(false);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento schermata Login", e);
            showAlert(Alert.AlertType.ERROR, "Errore durante il logout: " + e.getMessage());
        }
    }

    /* ========================= Helpers ========================= */

    private boolean ensureUserLoggedIn() {
        Integer uid = Session.getUserId();
        if (uid != null) return true;
        showAlert(Alert.AlertType.WARNING, "Utente non loggato. Effettua l’accesso.");
        disableAll();
        return false;
    }

    private boolean loadSellerShop() {
        try {
            var shop = SellerDAO.findShopForUser(Session.getUserId());
            if (shop == null) {
                showAlert(Alert.AlertType.ERROR, "Nessun negozio associato al venditore.");
                disableAll();
                return false;
            }
            currentShopId = shop.shopId();
            shopNameLabel.setText(shop.shopName() == null ? ("Negozio #" + currentShopId) : shop.shopName());
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore lookup shop del venditore", e);
            showAlert(Alert.AlertType.ERROR, "Errore nel recupero del negozio.");
            disableAll();
            return false;
        }
    }

    private void populateOrderStates() {
        orderStateFilter.getItems().setAll(ORDER_STATES);
        orderStateCombo.getItems().setAll(ORDER_STATES);
    }

    private void setupPlaceholders() {
        if (productsTable != null) productsTable.setPlaceholder(new Label("Nessun prodotto"));
        if (sellerOrdersTable != null) sellerOrdersTable.setPlaceholder(new Label("Nessun ordine"));
        if (orderItemsTable != null) orderItemsTable.setPlaceholder(new Label("Nessun articolo"));
    }

    private void wireFilterListeners() {
        attachReloadOnChange(brandFilter);
        attachReloadOnChange(categoryFilter);
    }

    private void attachReloadOnChange(ComboBox<String> cb) {
        if (cb == null) return;
        cb.valueProperty().addListener((obs, ov, nv) -> {
            if (!updatingFilters) reloadCatalog();
        });
    }

    private void wireOrderSelection() {
        if (sellerOrdersTable == null) return;
        sellerOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) loadOrderLines(sel.orderId());
            else orderItemsTable.getItems().clear();
        });
    }

    private void bootstrapData() {
        reloadCatalog();
        reloadOrders(null);
    }

    private void reloadCatalog() {
        runAsync(
                () -> SellerDAO.listCatalog(currentShopId, null),
                rows -> {
                    String nameQ = nullIfBlank(productSearchField != null ? productSearchField.getText() : null);
                    List<SellerDAO.CatalogRow> byName = (nameQ == null)
                            ? rows
                            : rows.stream()
                            .filter(r -> {
                                String n = r.name();
                                return n != null && n.toLowerCase(Locale.ITALIAN)
                                        .contains(nameQ.toLowerCase(Locale.ITALIAN));
                            })
                            .toList();

                    updatingFilters = true;
                    try {
                        updateCatalogFiltersOptions(byName);
                    } finally {
                        updatingFilters = false;
                    }

                    String selBrand = (brandFilter == null) ? null : brandFilter.getValue();
                    String selCat = (categoryFilter == null) ? null : categoryFilter.getValue();

                    var filtered = byName.stream()
                            .filter(r -> selBrand == null || selBrand.equals(r.brand()))
                            .filter(r -> selCat == null || selCat.equals(r.category()))
                            .toList();

                    productsTable.setItems(observableArrayList(filtered));
                    forceLayout(productsTable);
                },
                e -> showAlert(Alert.AlertType.ERROR, "Errore nel caricamento catalogo: " + e.getMessage())
        );
    }

    private void updateCatalogFiltersOptions(List<SellerDAO.CatalogRow> rows) {
        updateFilter(brandFilter, rows, SellerDAO.CatalogRow::brand);
        updateFilter(categoryFilter, rows, SellerDAO.CatalogRow::category);
    }

    private static void updateFilter(ComboBox<String> combo,
                                     List<SellerDAO.CatalogRow> rows,
                                     Function<SellerDAO.CatalogRow, String> keyExtractor) {
        if (combo == null) return;

        String current = combo.getValue();

        var options = rows.stream()
                .map(keyExtractor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        if (!combo.getItems().equals(options)) {
            combo.getItems().setAll(options);
        }

        if (current != null && options.contains(current)) {
            combo.getSelectionModel().select(current);
        } else {
            combo.getSelectionModel().clearSelection();
        }
    }

    private void reloadOrders(String stateFilter) {
        runAsync(
                () -> SellerDAO.listShopOrders(currentShopId, stateFilter),
                rows -> {
                    sellerOrdersTable.setItems(observableArrayList(rows));
                    orderItemsTable.getItems().clear();
                    if (!rows.isEmpty()) {
                        sellerOrdersTable.getSelectionModel().selectFirst();
                    }
                    forceLayout(sellerOrdersTable);
                    forceLayout(orderItemsTable);
                },
                e -> showAlert(Alert.AlertType.ERROR, "Errore nel caricamento ordini: " + e.getMessage())
        );
    }

    private void loadOrderLines(int orderId) {
        runAsync(
                () -> SellerDAO.listShopOrderLines(currentShopId, orderId),
                lines -> {
                    orderItemsTable.setItems(observableArrayList(lines));
                    forceLayout(orderItemsTable);
                },
                ex -> showAlert(Alert.AlertType.ERROR, "Errore nel caricamento dettagli ordine: " + ex.getMessage())
        );
    }

    private static void setupPromptCombo(ComboBox<String> cb, String prompt) {
        if (cb == null) return;
        cb.setPromptText(prompt);
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? cb.getPromptText() : item);
            }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
    }

    private void installTableFixes() {
        applyHeaderFix(productsTable);
        applyHeaderFix(sellerOrdersTable);
        applyHeaderFix(orderItemsTable);
    }

    private void applyHeaderFix(TableView<?> tv) {
        tv.skinProperty().addListener((obs, old, skin) -> {
            if (skin != null) forceLayout(tv);
        });
        tv.getItems().addListener((InvalidationListener) c -> forceLayout(tv));
        tv.widthProperty().addListener((o, old, w) -> forceLayout(tv));
    }

    private void forceLayout(TableView<?> tv) {
        Platform.runLater(() -> {
            tv.refresh();
            tv.layout();
            Platform.runLater(tv::layout);
        });
    }

    private void disableAll() {
        if (tabPane != null) tabPane.setDisable(true);
    }

    private void showAlert(Alert.AlertType t, String msg) {
        Alert a = new Alert(t);
        a.setHeaderText(null);
        a.setContentText(msg);
        if (logoutButton != null && logoutButton.getScene() != null) {
            a.initOwner(logoutButton.getScene().getWindow());
        }
        a.showAndWait();
    }

    private static String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal nonNull(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    /* ============== Dialog per Aggiungi/Modifica catalogo ============== */

    private record CatalogForm(int productId, String size, BigDecimal price, int quantity) {}

    private Dialog<CatalogForm> buildCatalogDialog(String title, CatalogForm initial) {
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
            loadAllProducts(ui.combo);
        } else {
            prefillEditMode(initial, ui);
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

    /* ------------------------- Helper di struttura ------------------------- */

    private Dialog<CatalogForm> createBaseDialog(String title) {
        Dialog<CatalogForm> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setResizable(true);
        if (logoutButton != null && logoutButton.getScene() != null) {
            dialog.initOwner(logoutButton.getScene().getWindow());
        }
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

    private VBox buildHeader(String title, String subtitle) {
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

    /* ------------------------- UI holder & stili ------------------------- */

    private record ProductUI(ComboBox<SellerDAO.ProductOption> combo,
                             TextField name,
                             TextField size,
                             TextField price,
                             TextField qty) {}

    private ProductUI createProductUI(CatalogForm initial) {
        ComboBox<SellerDAO.ProductOption> cb = new ComboBox<>();
        cb.setEditable(true);
        cb.setPromptText("Cerca o seleziona prodotto");
        cb.setConverter(new StringConverter<>() {
            @Override
            public String toString(SellerDAO.ProductOption item) {
                return item == null ? "" : item.toString();
            }

            @Override
            public SellerDAO.ProductOption fromString(String s) {
                return cb.getValue();
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SellerDAO.ProductOption item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? cb.getPromptText() : item.toString());
            }
        });
        cb.setCellFactory(lv -> {
            ListCell<SellerDAO.ProductOption> cell = new ListCell<>() {
                @Override
                protected void updateItem(SellerDAO.ProductOption item, boolean empty) {
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

    private record Styles(String base, String hover, String focus, String error) {}

    private Styles makeStyles() {
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

    private void applyFieldStyles(ProductUI ui, Styles s, boolean isAdd) {
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

    /* ------------------------- Layout ------------------------- */

    private GridPane buildFormGrid(ProductUI ui, boolean isEdit) {
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

    private static final String BTN_STYLE_FMT = """
        -fx-background-color: %s; -fx-text-fill: white; -fx-font-weight: bold;
        -fx-background-radius: %s; -fx-padding: 10 16; -fx-cursor: hand;
        """;

    private String computeOkBtnColor(boolean hover, boolean pressed) {
        if (pressed) return ACCENT_PRES;
        if (hover) return ACCENT_HOV;
        return ACCENT;
    }

    private void applyOkBtnStyle(Button b, boolean hover, boolean pressed) {
        b.setStyle(BTN_STYLE_FMT.formatted(computeOkBtnColor(hover, pressed), RADIUS_SM));
    }

    private String computeCancelBtnBg(boolean hover, boolean pressed) {
        if (pressed) return NEUTRAL_PRES;
        if (hover) return NEUTRAL_HOV;
        return SURFACE;
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

    private void styleButtons(DialogPane pane) {
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

    /* ------------------------- Validazione & result ------------------------- */

    private void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                if (initial == null) ui.combo.getEditor().setStyle(styles.base);
                ui.size.setStyle(styles.base);
                ui.price.setStyle(styles.base);
                ui.qty.setStyle(styles.base);

                if (initial == null && ui.combo.getValue() == null) {
                    ui.combo.getEditor().setStyle(styles.error);
                    throw new IllegalArgumentException("Seleziona un prodotto cliccando sulla lista");
                }
                if (!ui.size.isDisabled() && ui.size.getText().isBlank()) {
                    ui.size.setStyle(styles.error);
                    throw new IllegalArgumentException("Taglia obbligatoria");
                }
                new BigDecimal(ui.price.getText().trim());
                Integer.parseInt(ui.qty.getText().trim());
            } catch (Exception ex) {
                ev.consume();
                showAlert(Alert.AlertType.WARNING, "Dati non validi: " + ex.getMessage());
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                int pid = (initial != null) ? initial.productId() : ui.combo.getValue().productId();
                String size = ui.size.getText().trim();
                BigDecimal price = new BigDecimal(ui.price.getText().trim());
                int qty = Integer.parseInt(ui.qty.getText().trim());
                return new CatalogForm(pid, size, price, qty);
            }
            return null;
        });
    }

    /* ------------------------- Modalità "Aggiungi" ------------------------- */

    private void setupAddModeHandlers(ComboBox<SellerDAO.ProductOption> cb) {
        bindEditorToValue(cb);
        installShowAllOnOpen(cb);
        blockEnterCommit(cb);
        dropValueOnTyping(cb);
        fixPopupSpaceIssue(cb);
        attachTypeahead(cb);
        syncEditorOnBlur(cb);
    }

    private void installShowAllOnOpen(ComboBox<SellerDAO.ProductOption> cb) {
        cb.showingProperty().addListener((obs, was, showing) -> {
            if (!Boolean.TRUE.equals(showing)) return;
            if (normalizeQuery(cb.getEditor().getText()).isEmpty()) {
                loadAllProducts(cb);
            }
        });
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

            String q = extractNameForSearch(neu);
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
            if (cb.isShowing()) loadAllProducts(cb);
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
        debounce.setOnFinished(evt -> runAsync(
                () -> SellerDAO.listProductOptionsByNameLike(first, 100),
                opts -> {
                    var filtered = opts.stream().filter(o2 -> matchesAllTokens(o2, tokens)).toList();
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
                    // Intentionally left blank:
                    // gli altri tasti non richiedono gestione qui.
                    // Il comportamento (typeahead, invio bloccato, ecc.) è gestito altrove.
                }
            }
        });
    }

    /* ------------------------- Utility locali ------------------------- */

    private static String extractNameForSearch(String s) {
        if (s == null) return "";
        String t = s;
        int dot = t.indexOf('·');
        if (dot >= 0) t = t.substring(0, dot);
        int par = t.indexOf('(');
        if (par >= 0) t = t.substring(0, par);
        return normalizeQuery(t);
    }

    private static String normalizeQuery(String s) {
        return (s == null ? "" : s)
                .toLowerCase(Locale.ITALIAN)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean matchesAllTokens(SellerDAO.ProductOption opt, String[] tokens) {
        String name = normalizeQuery(opt.name());
        for (String t : tokens) {
            if (!t.isBlank() && !name.contains(t)) return false;
        }
        return true;
    }

    private void loadAllProducts(ComboBox<SellerDAO.ProductOption> cb) {
        runAsync(
                () -> SellerDAO.listProductOptionsByNameLike("", 100),
                opts -> {
                    cb.getItems().setAll(opts);
                    if (cb.isShowing()) cb.show();
                },
                ex -> showAlert(Alert.AlertType.ERROR, "Errore nel caricamento prodotti: " + ex.getMessage())
        );
    }

    /* ------------------------- Modalità "Modifica" ------------------------- */

    private void prefillEditMode(CatalogForm initial, ProductUI ui) {
        ui.size.setText(initial.size());
        ui.size.setDisable(true);
        ui.price.setText(initial.price().toPlainString());
        ui.qty.setText(String.valueOf(initial.quantity()));
        runAsync(
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

    /* ========================= Utility async ========================= */

    private <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        EXEC.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Errore operazione async", ex);
                Platform.runLater(() -> onError.accept(ex instanceof SQLException ? ex : new Exception(ex)));
            }
        });
    }

    private void runAsync(Callable<Void> task, Runnable onSuccess, Consumer<Exception> onError) {
        runAsync(task, v -> onSuccess.run(), onError);
    }

    private void refreshBalance()  {
        Integer userId = Session.getUserId();

        try{
            BigDecimal bal = ShopDAO.getBalance(userId);
            balanceLabel.setText(currencyIt.format(bal));
            withdrawButton.setDisable((bal != null ? bal.compareTo(BigDecimal.ZERO) : 0) <= 0);
        }catch (SQLException e) {
            logger.info( "Aggiornamento saldo fallito");
            balanceLabel.setText("—");
            withdrawButton.setDisable(true);
            new Alert(Alert.AlertType.ERROR, "Impossibile aggiornare il saldo: " + e.getMessage()).showAndWait();
        }

    }

    @FXML
    private void onWithdraw() {
        Integer userId = Session.getUserId();
        if (userId == null) {
            new Alert(Alert.AlertType.INFORMATION, "Devi effettuare il login.").showAndWait();
            return;
        }

        try {
            BigDecimal saldo = ShopDAO.getBalance(userId);
            if ((saldo != null ? saldo.compareTo(BigDecimal.ZERO) : 0) <= 0) {
                new Alert(Alert.AlertType.INFORMATION, "Saldo non disponibile.").showAndWait();
                return;
            }

            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/WithdrawSelection.fxml"));
            Parent root = l.load();
            WithdrawSelectionController ctrl = l.getController();

            Stage dialog = new Stage();
            dialog.initOwner(balanceLabel.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Prelievo");

            ctrl.setStage(dialog);
            ctrl.setUserId(userId);
            ctrl.setOnWithdrawDone(this::refreshBalance);

            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            refreshBalance();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossibile aprire la finestra di prelievo: " + e.getMessage()).showAndWait();
        }
    }

}
