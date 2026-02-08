
package org.example.controllers.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.controlsfx.control.RangeSlider;
import org.example.controllers.app.*;
import org.example.models.FilterCriteria;
import org.example.models.Product;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController implements Initializable {

    private HomeAppController appController;

    public HomeController() {
        // costruttore usato da FXMLLoader
    }

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());
    private static final String ALL = "Tutti";
    private static final String LAST = "Ultimi arrivi";
    private Popup cartPopup;
    private Popup profilePopup;
    private Popup wishesPopup;

    @FXML private StackPane cartBadgeContainer;
    @FXML private Button cartBtn;
    @FXML private Button profileBtn;
    @FXML private Button wishesBtn;
    @FXML private ComboBox<String> sportFilter;
    @FXML private ComboBox<String> brandFilter;
    @FXML private ComboBox<String> shopFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private RangeSlider priceRangeSlider;
    @FXML private Label priceRangeLabel;
    @FXML private TextField searchField;
    @FXML private Label welcomeLabel;
    @FXML private TilePane productPane;
    @FXML private Label sectionTitle;

    public void setAppController(HomeAppController appController) {
        this.appController = appController;
        welcomeLabel.setText("Benvenuto, " + appController.getCurrentUserName() + "!");
        updateCart();

        loadLatestArrivals();
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sectionTitle.setText(LAST);

        if (appController != null) loadLatestArrivals();

        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.selectAll();
        });

        // Setto i flitri
        sportFilter.getItems().addAll(ALL, "Calcio","Basket","Running","Tennis","Nuoto");
        sportFilter.setValue(ALL);

        brandFilter.getItems().addAll(ALL, "Adidas","Nike","Puma","Joma","Jordan");
        brandFilter.setValue(ALL);

        shopFilter.getItems().addAll(ALL, "Cisalfa Sport","Decathlon","Sport Incontro","Under Armour","JD Sports");
        shopFilter.setValue(ALL);

        categoryFilter.getItems().addAll(ALL, "Calzature","Abbigliamento","Accessori");
        categoryFilter.setValue(ALL);

        priceRangeSlider.lowValueProperty().addListener((obs, o, n) -> updatePriceLabel());
        priceRangeSlider.highValueProperty().addListener((obs, o, n) -> updatePriceLabel());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() >= 3) {
                searchProducts(newValue);
            } else {
                sectionTitle.setText(LAST);
                loadLatestArrivals();
            }
        });
    }

    // Ricerca prodotto dopo il 3 carattere digitato
    @FXML
    private void searchProducts(String query) {
        sectionTitle.setText("Risultati per: \"" + query + "\"");
        productPane.getChildren().clear();

        try {
            List<Product> results = appController.searchByName(query);

            if (results.isEmpty()) {
                Label noResults = new Label("Nessun prodotto trovato per \"" + query + "\"");
                productPane.getChildren().add(noResults);
                return;
            }

            displayProducts(results);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore nella ricerca dei prodotti", e);
            showAlert("Errore durante la ricerca: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        appController.logout();
        switchToLoginScene();
    }

    private void switchToLoginScene() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                Parent root = loader.load();

                LoginController loginUI = loader.getController();
                if (loginUI != null) {
                    loginUI.setAppController(new LoginAppController());
                }

                Stage stage = getCurrentStage();
                if (stage != null) stage.setScene(new Scene(root));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Errore navigazione login", e);
                showAlert("Errore logout: " + e.getMessage());
            }
        });
    }

    @FXML
    public void onCart() {
        try {
            if (cartPopup != null && cartPopup.isShowing()) {
                cartPopup.hide();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Cart.fxml"));
            VBox popupContent = loader.load();
            CartController controller = loader.getController();
            controller.setOnCartUpdated(this::updateCart);
            controller.setAppController(new CartAppController());

            cartPopup = new Popup();
            cartPopup.getContent().add(popupContent);
            cartPopup.setAutoHide(true);

            cartPopup.show(cartBtn, 0, 0);

            Runnable recenter = () -> {
                if (cartPopup == null || !cartPopup.isShowing()) return;
                popupContent.applyCss();
                popupContent.layout();
                var btnB = cartBtn.localToScreen(cartBtn.getBoundsInLocal());
                double pw = popupContent.getLayoutBounds().getWidth();
                double x  = btnB.getMinX() + (btnB.getWidth() - pw) / 2.0;
                double y  = btnB.getMaxY();
                cartPopup.setX(x);
                cartPopup.setY(y);
            };
            recenter.run();
            popupContent.layoutBoundsProperty().addListener((obs, o, n) -> recenter.run());
            controller.loadCartItems();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel caricamento del menu carrello", e);
            showAlert("Errore caricamento menu carrello: " + e.getMessage());
        }
    }

    public void updateCart() {
        Platform.runLater(() -> {
            int count = appController.getCartCount();

            if (count > 0) {
                ((Label)cartBadgeContainer.getChildren().get(1)).setText(String.valueOf(count));
                cartBadgeContainer.setVisible(true);
            } else {
                cartBadgeContainer.setVisible(false);
            }
        });
    }

    @FXML
    public void onProfile() {
        try {
            if (profilePopup != null && profilePopup.isShowing()) {
                profilePopup.hide();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Profile.fxml"));
            Parent dropdownContent = loader.load();
            ProfileController controller = loader.getController();

            profilePopup = new Popup();
            profilePopup.getContent().add(dropdownContent);
            profilePopup.setAutoHide(true);

            controller.setOnProfileDetails(() -> { openProfileDetails(); profilePopup.hide(); });
            controller.setOnPurchaseHistory(() -> { openPurchaseHistory(); profilePopup.hide(); });
            controller.setOnSavedCards(() -> { openSavedCards(); profilePopup.hide(); });

            Bounds bounds = profileBtn.localToScreen(profileBtn.getBoundsInLocal());
            dropdownContent.applyCss();
            dropdownContent.layout();

            double popupWidth = dropdownContent.prefWidth(-1);
            double centerX = bounds.getMinX() + bounds.getWidth() / 2;
            double x = centerX - popupWidth / 2;
            double y = bounds.getMaxY();
            profilePopup.show(profileBtn, x, y);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel caricamento del menu profilo", e);
            showAlert("Errore caricamento menu profilo: " + e.getMessage());
        }
    }

    private void openProfileDetails() { openSidePanel("fxml/ProfileDetails.fxml"); }
    private void openPurchaseHistory() { openSidePanel("fxml/PurchaseHistory.fxml"); }
    private void openSavedCards() { openSidePanel("fxml/SavedCards.fxml"); }

    @FXML public void onWishes() {
        try {
            if (wishesPopup != null && wishesPopup.isShowing()) {
                wishesPopup.hide();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Wishlist.fxml"));
            VBox popupContent = loader.load();
            WishlistController controller = loader.getController();
            controller.setOnCartUpdated(this::updateCart);
            controller.setAppController(new WishlistAppController());

            Popup p = new Popup();
            p.getContent().add(popupContent);
            p.setAutoHide(true);
            wishesPopup = p;

            Bounds b = wishesBtn.localToScreen(wishesBtn.getBoundsInLocal());
            popupContent.applyCss();
            popupContent.layout();
            double w = popupContent.prefWidth(-1);
            double x = b.getMinX() + b.getWidth()/2 - w/2;
            double y = b.getMaxY();
            p.show(wishesBtn, x, y);

        } catch (IOException e) {
            logger.log(Level.SEVERE,"Errore nel caricamento del menu wishlist", e);
            showAlert("Impossibile aprire la Wish List: " + e.getMessage());
        }
    }

    private void updatePriceLabel() {
        int low = (int) priceRangeSlider.getLowValue();
        int high = (int) priceRangeSlider.getHighValue();
        priceRangeLabel.setText(low + " € - " + high + " €");
    }

    private void loadLatestArrivals() {
        try {
            List<Product> latest = appController.findLatest(40);
            displayProducts(latest);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
            showAlert("Errore nel caricamento dei prodotti: " + e.getMessage());
        }
    }

    // Carica i prodotti in base ai filtri selezionati
    @FXML public void onFilter() {
        FilterCriteria criteria = appController.createFilterCriteria(
                sportFilter.getValue(),
                brandFilter.getValue(),
                shopFilter.getValue(),
                categoryFilter.getValue(),
                priceRangeSlider.getLowValue(),
                priceRangeSlider.getHighValue()
        );

        try {
            List<Product> filteredProducts = appController.searchByFilters(criteria);
            int min = (int) criteria.minPrice();
            int max = (int) criteria.maxPrice();
            sectionTitle.setText("Filtrati per: " + criteria.sport() + ", " + criteria.brand() +
                    ", " + criteria.shop() + ", " + criteria.category() + ", Prezzo: " +
                    min + " - " + max);
            displayProducts(filteredProducts.isEmpty() ? List.of() : filteredProducts);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il filtraggio dei prodotti", e);
            showAlert("Errore nel filtraggio: " + e.getMessage());
        }
    }

    @FXML
    public void onResetFilter() {
        FilterCriteria defaults = appController.resetFilters();

        // Settaggio valori
        sportFilter.setValue(defaults.sport());
        brandFilter.setValue(defaults.brand());
        shopFilter.setValue(defaults.shop());
        categoryFilter.setValue(defaults.category());
        priceRangeSlider.setLowValue(defaults.minPrice());
        priceRangeSlider.setHighValue(defaults.maxPrice());
        updatePriceLabel();

        sectionTitle.setText(LAST);
        loadLatestArrivals();
    }

    // Carica le card dei prodotti nella home
    private void displayProducts(List<Product> products) {
        productPane.getChildren().clear();
        for (Product p : products) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
                Node card = loader.load();
                ProductCardController ctrl = loader.getController();
                appController.createProductCard(ctrl, p);
                ctrl.setOnCartUpdate(this::updateCart);
                productPane.getChildren().add(card);
            } catch (IOException e){
                logger.log(Level.WARNING, "Errore nel caricamento dei prodotti", e);
            }
        }
    }

    private Stage getCurrentStage() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;  // Fallback
    }

    private void openSidePanel(String fxmlResource) {
        if (fxmlResource == null || fxmlResource.trim().isEmpty()) {
            logger.log(Level.WARNING, "Nome FXML non valido: {0}", fxmlResource);
            showAlert("Errore interno: schermata non valida.");
            return;
        }
        URL resource = getClass().getResource("/" + fxmlResource);
        if (resource == null) {
            logger.log(Level.WARNING, "File FXML non valido: {0}", fxmlResource);
            showAlert("Schermata non trovata: " + fxmlResource);
            return;
        }
        try {
            Stage stage = getStage(fxmlResource, resource);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento finestra", e);
            showAlert("Errore caricamento finestra: " + e.getMessage());
        }
    }

    private static Stage getStage(String fxmlResource, URL resource) throws IOException {
        FXMLLoader loader = new FXMLLoader(resource);
        Parent content = loader.load();
        Scene scene = new Scene(content);
        Stage stage = new Stage();
        stage.setScene(scene);

        Object ctrl = loader.getController();
        if (fxmlResource.contains("ProfileDetails")){
            ((ProfileDetailsController)ctrl).setAppController(new ProfileDetailsAppController());
            stage.setTitle("Dettagli Profile");
        }
        else if (fxmlResource.contains("SavedCards")){
            ((SavedCardsController)ctrl).setAppController(new SavedCardsAppController());
            stage.setTitle("Carte Salvate");
        }
        else if (fxmlResource.contains("PurchaseHistory")){
            ((PurchaseHistoryController)ctrl).setAppController(new PurchaseHistoryAppController());
            stage.setTitle("Storico Acquisti");
        }
        else {
            stage.setTitle("E-Shop");
        }
        stage.setResizable(false);
        stage.centerOnScreen();
        return stage;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
