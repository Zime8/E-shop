package org.example.boundary;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.controlsfx.control.RangeSlider;
import org.example.control.*;
import org.example.models.dto.FilterCriteria;
import org.example.models.dto.ProductDto;
import org.example.util.Navigator;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController implements Initializable {

    private BuyProductController appController;
    private ReviewAppController reviewAppController;
    private Navigator navigator;
    private boolean uiReady = false;
    private boolean initialLoadDone = false;

    public HomeController() {
        // costruttore usato da FXMLLoader
    }

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());
    private static final String ALL = "Tutti";
    private static final String LAST = "Ultimi arrivi";
    private Popup cartPopup;
    private Popup profilePopup;
    private Popup wishesPopup;

    @FXML private Label cartBadgeLabel;
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

    public void setAppController(BuyProductController appController) {
        this.appController = appController;
        tryInitialLoad();
    }

    public void setReviewAppController(ReviewAppController reviewAppController) {
        this.reviewAppController = reviewAppController;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
        tryInitialLoad();
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        uiReady = true;

        sectionTitle.setText(LAST);

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

        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.selectAll();
        });

        tryInitialLoad();
    }

    private void tryInitialLoad() {
        if (!uiReady || initialLoadDone || appController == null) return;

        welcomeLabel.setText("Benvenuto, " + appController.getCurrentUsername() + "!");
        updateCart();
        loadLatestArrivals();

        initialLoadDone = true;
    }

    // Ricerca prodotto dopo il 3 carattere digitato
    @FXML
    private void searchProducts(String query) {
        sectionTitle.setText("Risultati per: \"" + query + "\"");
        productPane.getChildren().clear();

        try {
            if (appController == null) return;
            List<ProductDto> results = appController.searchByName(query);

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
        if (appController == null || navigator == null) return;
        appController.logout();
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        navigator.goToLogin(stage);
    }

    @FXML
    public void onCart() {
        try {
            if (appController == null || navigator == null) return;
            if (cartPopup != null && cartPopup.isShowing()) {
                cartPopup.hide();
                return;
            }
            FXMLLoader loader = navigator.createAppLoader("/fxml/Cart.fxml");
            VBox popupContent = loader.load();
            CartController controller = loader.getController();
            controller.setOnCartUpdated(this::updateCart);
            controller.setAppController(appController);
            controller.setNavigator(navigator);

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
            controller.refreshView();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore nel caricamento del menu carrello", e);
            showAlert("Errore caricamento menu carrello: " + e.getMessage());
        }
    }

    public void updateCart() {
        Platform.runLater(() -> {
            if (appController == null) return;

            int count = appController.getCartCount();
            cartBadgeLabel.setText(String.valueOf(count));

            boolean hasItems = count > 0;
            cartBadgeLabel.setVisible(hasItems);
            cartBadgeLabel.setManaged(hasItems);
            cartBadgeLabel.toFront();
        });
    }

    @FXML
    public void onProfile() {
        try {
            if (appController == null || navigator == null) return;
            if (profilePopup != null && profilePopup.isShowing()) {
                profilePopup.hide();
                return;
            }
            FXMLLoader loader = navigator.createAppLoader("/fxml/Profile.fxml");
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

    private void openProfileDetails() {
        navigator.openModal("/fxml/ProfileDetails.fxml", (ProfileDetailsController controller) ->
                controller.setAppController(appController.getModifyProfileController()));
    }
    private void openPurchaseHistory() {
        navigator.openModal("/fxml/PurchaseHistory.fxml", (PurchaseHistoryController controller) ->
                controller.setAppController(appController.getCheckOrdersController()));
    }
    private void openSavedCards() {
        navigator.openModal("/fxml/SavedCards.fxml", (SavedCardsController controller) ->
                controller.setAppController(appController.getModifyProfileController()));
    }

    @FXML public void onWishes() {
        try {
            if (appController == null || navigator == null) return;
            if (wishesPopup != null && wishesPopup.isShowing()) {
                wishesPopup.hide();
                return;
            }
            FXMLLoader loader = navigator.createAppLoader("/fxml/Wishlist.fxml");
            VBox popupContent = loader.load();
            WishlistController controller = loader.getController();
            controller.setOnCartUpdated(this::updateCart);
            controller.setAppController(appController.getWishlistController());

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
            if (appController == null || navigator == null) return;
            List<ProductDto> latest = appController.findLatest(40);
            displayProducts(latest);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
            showAlert("Errore nel caricamento dei prodotti: " + e.getMessage());
        }
    }

    // Carica i prodotti in base ai filtri selezionati
    @FXML public void onFilter() {
        if (appController == null) return;
        FilterCriteria criteria = appController.createFilterCriteria(
                sportFilter.getValue(),
                brandFilter.getValue(),
                shopFilter.getValue(),
                categoryFilter.getValue(),
                priceRangeSlider.getLowValue(),
                priceRangeSlider.getHighValue()
        );

        try {
            List<ProductDto> filteredProducts = appController.searchByFilters(criteria);
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
        if (appController == null) return;
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
    private void displayProducts(List<ProductDto> products) {
        productPane.getChildren().clear();
        if(appController == null || navigator == null) return;
        for (ProductDto p : products) {
            try {
                FXMLLoader loader = navigator.createAppLoader("/fxml/ProductCard.fxml");
                Node card = loader.load();
                ProductCardController ctrl = loader.getController();
                ctrl.setAppController(appController);
                ctrl.setReviewAppController(reviewAppController);
                ctrl.setNavigator(navigator);
                ctrl.setProduct(p);
                ctrl.setOnCartUpdate(this::updateCart);
                productPane.getChildren().add(card);
            } catch (IOException e){
                logger.log(Level.WARNING, "Errore nel caricamento dei prodotti", e);
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
