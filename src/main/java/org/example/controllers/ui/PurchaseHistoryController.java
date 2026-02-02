package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.example.controllers.app.PurchaseHistoryAppController;
import org.example.controllers.app.PurchaseHistoryAppController.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static javafx.collections.FXCollections.observableArrayList;

public class PurchaseHistoryController {

    private static final String ALIGN_CENTER = "-fx-alignment: CENTER;";

    @FXML private Button closeButton;

    // Tabella ORDINI
    @FXML private TableView<OrderSummaryView> ordersTable;
    @FXML private TableColumn<OrderSummaryView, Number> colOrderId;
    @FXML private TableColumn<OrderSummaryView, String> colOrderDate;
    @FXML private TableColumn<OrderSummaryView, String> colOrderStatus;
    @FXML private TableColumn<OrderSummaryView, String> colOrderTotal;

    // Tabella DETTAGLI
    @FXML private TableView<OrderLineView> itemsTable;
    @FXML private TableColumn<OrderLineView, String> colItemName;
    @FXML private TableColumn<OrderLineView, String> colItemSize;
    @FXML private TableColumn<OrderLineView, String> colItemShop;
    @FXML private TableColumn<OrderLineView, Number> colItemQty;
    @FXML private TableColumn<OrderLineView, String> colItemPrice;
    @FXML private TableColumn<OrderLineView, String> colItemSubtotal;

    private final ObservableList<OrderSummaryView> orders = observableArrayList();
    private final ObservableList<OrderLineView> items = observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PurchaseHistoryAppController appController;

    @FXML
    private void initialize() {
        appController = new PurchaseHistoryAppController();
        setupOrdersTable();
        setupItemsTable();
        setupSelectionListener();

        // Delega caricamento dati
        appController.loadOrders(this::displayOrders, this::displayItems);
    }

    private void setupOrdersTable() {
        colOrderId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().idOrder()));
        colOrderDate.setCellValueFactory(cd -> new SimpleStringProperty(formatDate(cd.getValue().dateOrder())));
        colOrderStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stateOrder()));
        colOrderTotal.setCellValueFactory(cd -> new SimpleStringProperty(formatTotal(cd.getValue().totalAmount())));

        ordersTable.setItems(orders);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        applyStyle(ordersTable);
    }

    private void setupItemsTable() {
        colItemName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().productName()));
        colItemSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().size()));
        colItemShop.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shopName()));
        colItemQty.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().quantity()));
        colItemPrice.setCellValueFactory(cd -> new SimpleStringProperty(formatTotal(cd.getValue().unitPrice())));
        colItemSubtotal.setCellValueFactory(cd -> new SimpleStringProperty(formatTotal(cd.getValue().subtotal())));

        itemsTable.setItems(items);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        applyStyle(itemsTable);
    }

    private void setupSelectionListener() {
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                appController.loadItemsForOrder(sel.idOrder(), this::displayItems);
            } else {
                items.clear();
            }
        });
    }

    private void displayOrders(List<OrderSummaryView> orderSummaries) {
        Platform.runLater(() -> {
            orders.setAll(orderSummaries);
            if (!orders.isEmpty()) {
                ordersTable.getSelectionModel().selectFirst();
            }
            ordersTable.layout();
        });
    }

    private void displayItems(List<OrderLineView> orderLines) {
        Platform.runLater(() -> {
            items.setAll(orderLines);
            itemsTable.layout();
        });
    }

    private String formatDate(Timestamp ts) {
        return (ts == null) ? "" : dateFmt.format(ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private String formatTotal(BigDecimal bd) {
        return bd == null ? "0.00" : bd.toPlainString();
    }

    private void applyStyle(TableView<?> table) {
        // ORDINI (ordersTable)
        if (table == ordersTable) {
            colOrderId.setStyle(ALIGN_CENTER);
            colOrderDate.setStyle(ALIGN_CENTER);
            colOrderStatus.setStyle(ALIGN_CENTER);
            colOrderTotal.setStyle(ALIGN_CENTER);
        }
        // DETTAGLIO (itemsTable)
        if (table == itemsTable) {
            colItemName.setStyle(ALIGN_CENTER);
            colItemSize.setStyle(ALIGN_CENTER);
            colItemShop.setStyle(ALIGN_CENTER);
            colItemQty.setStyle(ALIGN_CENTER);
            colItemPrice.setStyle(ALIGN_CENTER);
            colItemSubtotal.setStyle(ALIGN_CENTER);
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
