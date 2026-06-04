    package org.example.boundary;

    import javafx.application.Platform;
    import javafx.beans.InvalidationListener;
    import javafx.beans.property.SimpleIntegerProperty;
    import javafx.beans.property.SimpleStringProperty;
    import javafx.fxml.FXML;
    import javafx.scene.control.*;
    import javafx.scene.control.Button;
    import javafx.scene.control.Label;
    import javafx.scene.control.TextField;
    import javafx.stage.Stage;
    import org.example.control.SellerOrdersController;
    import org.example.control.SellerProductsController;
    import org.example.boundary.dialogs.AddCatalogDialogCreator;
    import org.example.boundary.dialogs.EditCatalogDialogCreator;
    import org.example.control.SellerHomeAppController;
    import org.example.models.dto.CatalogForm;
    import org.example.models.dto.CatalogRow;
    import org.example.models.dto.ShopOrderLine;
    import org.example.models.dto.ShopOrderSummary;
    import org.example.util.Navigator;


    import java.awt.*;
    import java.math.BigDecimal;
    import java.net.URI;
    import java.net.URLEncoder;
    import java.nio.charset.StandardCharsets;
    import java.text.NumberFormat;
    import java.time.ZoneId;
    import java.time.format.DateTimeFormatter;
    import java.util.List;
    import java.util.Locale;
    import java.util.Objects;
    import java.util.function.Function;
    import java.util.logging.Level;
    import java.util.logging.Logger;

    import static javafx.collections.FXCollections.observableArrayList;

    public class SellerHomeController {

        private static final Logger logger = Logger.getLogger(SellerHomeController.class.getName());

        // CSS / Theme
        private static final String ALIGN_CENTER = "-fx-alignment: CENTER;";

        private boolean updatingFilters = false;

        // Header
        @FXML private Label shopNameLabel;
        @FXML private Button logoutButton;
        @FXML private TabPane tabPane;
        @FXML private Label balanceLabel;
        @FXML private Button withdrawButton;

        // Catalogo
        @FXML private TextField productSearchField;
        @FXML private ComboBox<String> brandFilter;
        @FXML private ComboBox<String> categoryFilter;
        @FXML private TableView<CatalogRow> productsTable;
        @FXML private TableColumn<CatalogRow, Number> colProdId;
        @FXML private TableColumn<CatalogRow, String> colProdName;
        @FXML private TableColumn<CatalogRow, String> colSport;
        @FXML private TableColumn<CatalogRow, String> colBrand;
        @FXML private TableColumn<CatalogRow, String> colCategory;
        @FXML private TableColumn<CatalogRow, String> colSize;
        @FXML private TableColumn<CatalogRow, String> colPrice;
        @FXML private TableColumn<CatalogRow, Number> colQuantity;

        // Ordini
        @FXML private ComboBox<String> orderStateFilter;
        @FXML private TableView<ShopOrderSummary> sellerOrdersTable;
        @FXML private TableColumn<ShopOrderSummary, Number> colOrderIdS;
        @FXML private TableColumn<ShopOrderSummary, String> colOrderDateS;
        @FXML private TableColumn<ShopOrderSummary, String> colOrderStateS;
        @FXML private TableColumn<ShopOrderSummary, String> colOrderTotalS;
        @FXML private TableColumn<ShopOrderSummary, String> colCustomerS;
        @FXML private TableColumn<ShopOrderSummary, String> colAddress;

        @FXML private TableView<ShopOrderLine> orderItemsTable;
        @FXML private TableColumn<ShopOrderLine, String> colItemNameS;
        @FXML private TableColumn<ShopOrderLine, String> colItemSizeS;
        @FXML private TableColumn<ShopOrderLine, String> colItemPriceS;
        @FXML private TableColumn<ShopOrderLine, String> colItemSubtotalS;
        @FXML private TableColumn<ShopOrderLine, Number> colItemQtyS;

        @FXML private ComboBox<String> orderStateCombo;

        private final DateTimeFormatter dateFmt =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
        private static final NumberFormat CURR_IT = NumberFormat.getCurrencyInstance(Locale.ITALY);

        private SellerHomeAppController appController;
        private Navigator navigator;
        private SellerOrdersController ordersController;
        private SellerProductsController productsController;

        public void setAppController(SellerHomeAppController app) {
            this.appController = app;
            tryInitialize();
        }

        public void setOrdersController(SellerOrdersController ordersController) {
            this.ordersController = ordersController;
            tryInitialize();
        }

        public void setProductsController(SellerProductsController productsController) {
            this.productsController = productsController;
            tryInitialize();
        }

        public void setNavigator(Navigator navigator) {
            this.navigator = navigator;
        }

        private boolean initialized = false;

        private void tryInitialize() {
            if (initialized) return;
            if (appController == null || ordersController == null || productsController == null) return;
            if (orderStateFilter == null || orderStateCombo == null) return;

            initialized = true;
            appController.populateOrderStates(orderStateFilter, orderStateCombo);
            loadSellerShop();
        }

        @FXML
        private void initialize() {
            setupPlaceholders();
            wireCatalogColumns();
            wireOrdersColumns();
            setupPromptCombo(brandFilter, "Marca");
            setupPromptCombo(categoryFilter, "Categoria");
            wireFilterListeners();
            wireOrderSelection();
            installTableFixes();

            tryInitialize();
        }

        public void onBalanceUpdated(BigDecimal balance) {
            if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
                balanceLabel.setText("-");
                withdrawButton.setDisable(true);
            } else {
                balanceLabel.setText(CURR_IT.format(balance));
                withdrawButton.setDisable(false);
            }
        }

        private void loadSellerShop() {
            appController.loadSellerShop(
                    shop ->{
                        shopNameLabel.setText(shop);
                        Integer shopId = appController.getCurrentShopId();
                        ordersController.setCurrentShopId(shopId);
                        productsController.setCurrentShopId(shopId);
                        appController.refreshBalance(this::onBalanceUpdated,
                                errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg));
                        bootstrapData();
                    },
                    errorMsg -> {
                        showAlert(Alert.AlertType.ERROR, errorMsg);
                        disableAll();
                    }
            );
        }

        private void bootstrapData(){
            reloadCatalog();
            reloadOrders(null);
        }

        // Bind colonne catalogo

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
            configureOrderSummaryColumns();
            colAddress.setCellFactory(tc -> new AddressCell());
            configureOrderItemColumns();
            applyOrderColumnsStyle();
        }

        // Catalogo

        @FXML
        private void onSearchProduct() {
            reloadCatalog();
        }

        @FXML
        private void onResetProductSearch() {
            productSearchField.clear();
            brandFilter.setValue(null);
            categoryFilter.setValue(null);
            reloadCatalog();
        }

        @FXML
        private void onAddProduct() {
            var dialog = new AddCatalogDialogCreator(logoutButton, productsController).newDialog();
            dialog.showAndWait().ifPresent(data ->
                    productsController.addProductAsync(data, this::reloadCatalog,
                            errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg)
                    )
            );
        }

        @FXML
        private void onEditProduct() {
            var sel = productsTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert(Alert.AlertType.INFORMATION, "Seleziona una riga da modificare.");
                return;
            }
            var dialog = new EditCatalogDialogCreator(
                    new CatalogForm(sel.productId(), sel.size(), appController.nonNull(sel.price()), sel.quantity()),
                    logoutButton, productsController
            ).newDialog();
            dialog.showAndWait().ifPresent(data ->
                    productsController.editProductAsync(data, this::reloadCatalog,
                            errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg))
            );
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
                    productsController.deleteProductAsync(sel.productId(), sel.size(), this::reloadCatalog,
                            errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg));
                }
            });
        }

        // Ordini

        @FXML
        private void onApplyOrderFilters() { reloadOrders(orderStateFilter.getValue()); }

        private void reloadOrders(String stateFilter){
            if(ordersController == null) return;

            ordersController.listOrderAsync(
                    stateFilter,
                    rows -> {
                        sellerOrdersTable.setItems(observableArrayList(rows));
                        orderItemsTable.getItems().clear();
                        if (!rows.isEmpty()){
                            sellerOrdersTable.getSelectionModel().selectFirst();
                        }
                        forceLayout(sellerOrdersTable);
                        forceLayout(orderItemsTable);
                    },
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg)
            );
        }

        @FXML
        private void onResetOrderFilters() {
            orderStateFilter.getSelectionModel().clearSelection();
            orderStateFilter.setValue(null);
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
            ordersController.updateOrderStatusAsync(sel.orderId(), st,
                    () -> reloadOrders(orderStateFilter.getValue()),
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg)
            );
        }

        private void configureOrderSummaryColumns() {
            colOrderIdS.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().orderId()));
            colOrderDateS.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().orderDate() == null ? "" : dateFmt.format(cd.getValue().orderDate().toInstant())
            ));
            colOrderStateS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().state()));
            colOrderTotalS.setCellValueFactory(cd -> new SimpleStringProperty(formatCurrency(cd.getValue().total())));
            colCustomerS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().customer()));
            colAddress.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().address()));
        }

        private void configureOrderItemColumns() {
            colItemNameS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().productName()));
            colItemSizeS.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().size()));
            colItemQtyS.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().quantity()));
            colItemPriceS.setCellValueFactory(cd -> new SimpleStringProperty(formatCurrency(cd.getValue().unitPrice())));
            colItemSubtotalS.setCellValueFactory(cd -> new SimpleStringProperty(formatCurrency(cd.getValue().subtotal())));
        }

        private void applyOrderColumnsStyle() {
            for (TableColumn<?, ?> c : List.of(
                    colOrderIdS, colOrderDateS, colOrderStateS, colOrderTotalS, colCustomerS, colAddress,
                    colItemNameS, colItemSizeS, colItemQtyS, colItemPriceS, colItemSubtotalS
            )) {
                c.setStyle(ALIGN_CENTER);
            }
        }

        // Cella per la colonna Address
        private final class AddressCell extends TableCell<ShopOrderSummary, String> {
            private final Hyperlink link = new Hyperlink();

            AddressCell() {
                setStyle(ALIGN_CENTER);
                link.setFocusTraversable(false);
                link.setOnAction(e -> {
                    var row = getTableView().getItems().get(getIndex());
                    String addr = (row == null) ? null : row.address();
                    if (addr != null && !addr.isBlank()) {
                        openMaps(addr);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText("-");
                } else {
                    link.setText(item);
                    setGraphic(link);
                    setText(null);
                }
            }
        }

        private void openMaps(String address) {
            try {
                String url = "https://www.google.com/maps/search/?api=1&query="
                        + URLEncoder.encode(address, StandardCharsets.UTF_8);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(url));
                } else {
                    // Fallback: mostra il link
                    new Alert(Alert.AlertType.INFORMATION,
                            "Apri l'indirizzo in un browser: " + url).showAndWait();
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Impossibile aprire Google Maps: " + ex.getMessage());
            }
        }

        private static String formatCurrency(BigDecimal v) {
            return CURR_IT.format(v == null ? BigDecimal.ZERO : v);
        }

        @FXML
        private void onLogout() {
            if (appController == null) return;

            if (appController.logout()) {
                if (navigator == null) {
                    showAlert(Alert.AlertType.ERROR, "Navigazione non disponibile.");
                    return;
                }

                Stage stage = (Stage) logoutButton.getScene().getWindow();
                navigator.goToLogin(stage);
            }
        }

        // Helpers

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
                if (!updatingFilters) {
                    reloadCatalog();
                }
            });
        }

        private void wireOrderSelection() {
            if (sellerOrdersTable == null) return;

            sellerOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                if (sel != null) {
                    if(ordersController == null) return;

                    ordersController.loadOrderLines(
                            sel.orderId(),
                            lines -> {
                                orderItemsTable.setItems(observableArrayList(lines));
                                forceLayout(orderItemsTable);
                            },
                            e -> showAlert(Alert.AlertType.ERROR, e)
                    );
                }
                else orderItemsTable.getItems().clear();
            });
        }

        private void reloadCatalog() {
            if(appController == null || productsController == null) return;

            productsController.reloadCatalog(
                    rawRows -> Platform.runLater(() -> updateCatalogFromData(rawRows)),
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg)
            );
        }

        private void updateCatalogFromData(List<CatalogRow> rows){
            String nameQ = appController.nullIfBlank(productSearchField.getText());
            List<CatalogRow> byName = (nameQ == null)
                    ? rows
                    : rows.stream().
                    filter(r -> {
                        String n = r.name();
                        return n != null && n.toLowerCase(Locale.ITALIAN)
                                .contains(nameQ.toLowerCase(Locale.ITALIAN));
                    }
            ).toList();

            updatingFilters = true;
            try {
                updateCatalogFiltersOptions(byName);
            } finally {
                updatingFilters = false;
            }

            // filtri brand/category
            String selBrand = (brandFilter == null) ? null : brandFilter.getValue();
            String selCategory = (categoryFilter == null) ? null : categoryFilter.getValue();

            var filtered = byName.stream()
                    .filter(r -> selBrand == null || selBrand.equals(r.brand()))
                    .filter(r -> selCategory == null || selCategory.equals(r.category()))
                    .toList();

            productsTable.setItems(observableArrayList(filtered));
            forceLayout(productsTable);
        }

        private void updateCatalogFiltersOptions(List<CatalogRow> rows) {
            updateFilter(brandFilter, rows, CatalogRow::brand);
            updateFilter(categoryFilter, rows, CatalogRow::category);
        }

        private static void updateFilter(ComboBox<String> combo,
                                         List<CatalogRow> rows,
                                         Function<CatalogRow, String> keyExtractor) {
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

        private void refreshBalance(){
            if(appController == null) return;
            appController.refreshBalance(
                    this::onBalanceUpdated,  // Nuovo callback
                    errorMsg -> showAlert(Alert.AlertType.ERROR, errorMsg)
            );
        }

        @FXML
        private void onWithdraw() {
            if(appController == null) return;

            appController.withdrawRequest(
                    () -> openWithdrawDialog(this::refreshBalance),
                    () -> showAlert(Alert.AlertType.INFORMATION, "Saldo insufficiente."),
                    () -> showAlert(Alert.AlertType.INFORMATION, "Login richiesto.")
            );
        }

        private void openWithdrawDialog(Runnable onSuccess) {
            if (navigator == null || appController == null) {
                showAlert(Alert.AlertType.ERROR, "Navigazione non disponibile.");
                return;
            }

            try {
                navigator.openModal(
                        "/fxml/WithdrawSelection.fxml",
                        (WithdrawSelectionController controller) -> {
                            controller.setUserId(appController.getCurrentUserId());
                            controller.setOnSuccess(onSuccess);
                        }
                );
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Errore dialog prelievo", e);
                showAlert(Alert.AlertType.ERROR, "Impossibile aprire finestra prelievo.");
            }
        }
    }
