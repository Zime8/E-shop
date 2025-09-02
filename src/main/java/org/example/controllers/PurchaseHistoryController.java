package org.example.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.example.dao.OrderDAO;
import org.example.dao.OrderDAO.OrderLine;
import org.example.dao.OrderDAO.OrderSummary;
import org.example.util.Session;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.collections.FXCollections.observableArrayList;

public class PurchaseHistoryController {

    private static final String ALIGN_CENTER = "-fx-alignment: CENTER;";

    @FXML private Button closeButton;

    // Tabella ORDINI
    @FXML private TableView<OrderSummary> ordersTable;
    @FXML private TableColumn<OrderSummary, Number> colOrderId;
    @FXML private TableColumn<OrderSummary, String> colOrderDate;
    @FXML private TableColumn<OrderSummary, String> colOrderStatus;
    @FXML private TableColumn<OrderSummary, String> colOrderTotal;

    // Tabella DETTAGLI
    @FXML private TableView<OrderLine> itemsTable;
    @FXML private TableColumn<OrderLine, String> colItemName;
    @FXML private TableColumn<OrderLine, String> colItemSize;
    @FXML private TableColumn<OrderLine, String> colItemShop;
    @FXML private TableColumn<OrderLine, Number> colItemQty;
    @FXML private TableColumn<OrderLine, String> colItemPrice;
    @FXML private TableColumn<OrderLine, String> colItemSubtotal;

    private final ObservableList<OrderSummary> orders = observableArrayList();
    private final ObservableList<OrderLine> items = observableArrayList();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Logger logger = Logger.getLogger(PurchaseHistoryController.class.getName());

    @FXML
    private void initialize() {
        // Bind colonne ORDINI
        colOrderId.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().idOrder()));
        colOrderDate.setCellValueFactory(cd -> {
            var ts = cd.getValue().dateOrder();
            String s = (ts == null) ? "" : dateFmt.format(ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            return new SimpleStringProperty(s);
        });
        colOrderStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().stateOrder()));
        colOrderTotal.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().totalAmount() == null ? "0.00" : cd.getValue().totalAmount().toPlainString()
        ));
        ordersTable.setItems(orders);

        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Quando selezioni un ordine, carica i dettagli
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                loadItems(sel.idOrder());
            } else {
                items.clear();
            }
        });

        // Bind colonne DETTAGLIO
        colItemName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().productName()));
        colItemSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().size()));
        colItemShop.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shopName()));
        colItemQty.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().quantity()));
        colItemPrice.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().unitPrice() == null ? "0.00" : cd.getValue().unitPrice().toPlainString()
        ));
        colItemSubtotal.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getSubtotal().toPlainString()
        ));

        // ORDINI
        colOrderId.setStyle(ALIGN_CENTER);
        colOrderDate.setStyle(ALIGN_CENTER);
        colOrderStatus.setStyle(ALIGN_CENTER);
        colOrderTotal.setStyle(ALIGN_CENTER);

        // DETTAGLIO
        colItemName.setStyle(ALIGN_CENTER);
        colItemSize.setStyle(ALIGN_CENTER);
        colItemShop.setStyle(ALIGN_CENTER);
        colItemQty.setStyle(ALIGN_CENTER);
        colItemPrice.setStyle(ALIGN_CENTER);
        colItemSubtotal.setStyle(ALIGN_CENTER);

        itemsTable.setItems(items);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Carica ordini per utente corrente
        Integer uid = Session.getUserId();
        if (uid == null) {
            logger.warning("Nessun utente loggato");
            showWarn();
            return;
        }
        Platform.runLater(() -> loadOrders(uid));
    }

    private void setLoading(boolean on) {
        if (ordersTable != null) ordersTable.setDisable(on);
        if (itemsTable != null) itemsTable.setDisable(on);
    }

    private void loadOrders(int userId) {
        setLoading(true);
        new Thread(() -> {
            try {
                List<OrderSummary> list = OrderDAO.getOrdersByUser(userId);
                Platform.runLater(() -> {
                    orders.setAll(list);
                    items.clear();
                    setLoading(false);
                    if (!orders.isEmpty()) {
                        ordersTable.getSelectionModel().selectFirst();
                    }
                    ordersTable.layout();
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Errore caricamente ordini", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Errore caricamento ordini", e.getMessage());
                });
            }
        }).start();
    }

    private void loadItems(int orderId) {
        setLoading(true);
        new Thread(() -> {
            try {
                List<OrderLine> list = OrderDAO.getOrderItems(orderId);
                Platform.runLater(() -> {
                    items.setAll(list);
                    setLoading(false);
                    itemsTable.layout();
                });
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Errore caricamento dettagli ordine", e);
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Errore caricamento dettagli ordine", e.getMessage());
                });
            }
        }).start();
    }

    private void showError(String header, String details) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Errore");
        a.setHeaderText(header);
        a.setContentText(details);
        a.showAndWait();
    }

    private void showWarn() {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attenzione");
        a.setHeaderText("Utente non loggato");
        a.setContentText("Accedi per visualizzare la cronologia ordini.");
        a.showAndWait();
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
