package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.controlsfx.control.RangeSlider;
import org.example.dao.ProductDaos;
import org.example.dao.api.ProductDao;
import org.example.demo.DemoData;
import org.example.models.Product;
import org.example.util.Session;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController implements Initializable {

    // ---- DI del DAO (Opzione 2)
    private final ProductDao productDao;
    public HomeController(ProductDao productDao) {      // costruttore per test/DI
        this.productDao = productDao;
    }

    public HomeController() {                           // costruttore usato da FXMLLoader
        this(ProductDaos.create());                     // DB o FS in base alle proprietà
    }

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());
    private static final String ALL = "Tutti";
    private Popup cartPopup;
    private Popup profilePopup;
    private Popup wishesPopup;

    @FXML private Label cartCountLabel;
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

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Benvenuto, " + Session.getUser() + "!");
        sectionTitle.setText("Ultimi Arrivi");

        loadLatestArrivals();

        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.selectAll();
        });

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
                sectionTitle.setText("Ultimi Arrivi");
                loadLatestArrivals();
            }
        });

        updateCart();
    }

    @FXML
    private void searchProducts(String query) {
        sectionTitle.setText("Risultati per: \"" + query + "\"");
        productPane.getChildren().clear();

        try {
            List<Product> results = productDao.searchByName(query); // << sostituisce ProductDAO.searchByName

            if (results.isEmpty()) {
                Label noResults = new Label("Nessun prodotto trovato per \"" + query + "\"");
                productPane.getChildren().add(noResults);
                return;
            }

            displayProducts(results);

        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Errore nella ricerca dei prodotti", e);
            showAlert("Errore durante la ricerca: " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        logger.info("Logout effettuato");
        try {
            if (Session.isDemo()) {
                DemoData.clearUserDemoReviews(Session.getUser());
            }
            Session.clear();

            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento schermata login", e);
            showAlert("Errore caricamento schermata login: " + e.getMessage());
        }
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
        List<Product> cartItems = Session.getCartItems();
        if (cartItems != null && !cartItems.isEmpty()) {
            cartCountLabel.setText(String.valueOf(cartItems.size()));
            cartCountLabel.setVisible(true);
            cartCountLabel.setManaged(true);
        } else {
            cartCountLabel.setVisible(false);
            cartCountLabel.setManaged(false);
        }
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

    public void onWishes() {
        try {
            if (wishesPopup != null && wishesPopup.isShowing()) {
                wishesPopup.hide();
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Wishlist.fxml"));
            VBox popupContent = loader.load();
            WishlistController controller = loader.getController();
            controller.setOnCartUpdated(this::updateCart);

            Popup p = new Popup();
            p.getContent().add(popupContent);
            p.setAutoHide(true);
            wishesPopup = p;

            controller.loadItems();

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
        productPane.getChildren().clear();
        try {
            List<Product> latest = productDao.findLatest(40);
            displayProducts(latest);
        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Errore durante il caricamento dei prodotti", e);
            showAlert("Errore nel caricamento dei prodotti: " + e.getMessage());
        }
    }

    public void onFilter() {
        String selectedSport = sportFilter.getValue();
        String selectedBrand = brandFilter.getValue();
        String selectedShop = shopFilter.getValue();
        String selectedCategory = categoryFilter.getValue();
        double minPrice = priceRangeSlider.getLowValue();
        double maxPrice = priceRangeSlider.getHighValue();

        sectionTitle.setText("Filtrati per: " + selectedSport + ", " + selectedBrand +
                ", " + selectedShop + ", " + selectedCategory + ", " + (int) minPrice + "€ - " + (int) maxPrice + "€");

        productPane.getChildren().clear();

        try {
            List<Product> filteredProducts = productDao.searchByFilters( // << sostituisce ProductDAO.searchByFilters
                    selectedSport.equals(ALL) ? null : selectedSport,
                    selectedBrand.equals(ALL) ? null : selectedBrand,
                    selectedShop.equals(ALL) ? null : selectedShop,
                    selectedCategory.equals(ALL) ? null : selectedCategory,
                    minPrice,
                    maxPrice
            );

            if (filteredProducts.isEmpty()) {
                productPane.getChildren().add(new Label("Nessun prodotto trovato con questi filtri."));
                return;
            }

            displayProducts(filteredProducts);

        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Errore durante il filtraggio dei prodotti", e);
            showAlert("Errore nel filtraggio: " + e.getMessage());
        }
    }

    public void onResetFilter() {
        sportFilter.setValue(ALL);
        brandFilter.setValue(ALL);
        shopFilter.setValue(ALL);
        categoryFilter.setValue(ALL);
        priceRangeSlider.setLowValue(priceRangeSlider.getMin());
        priceRangeSlider.setHighValue(priceRangeSlider.getMax());
        updatePriceLabel();
    }

    private void displayProducts(List<Product> products) throws IOException  {
        productPane.getChildren().clear();
        for (Product p : products) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductCard.fxml"));
            Node card = loader.load();
            ProductCardController ctrl = loader.getController();
            ctrl.setProduct(p);
            ctrl.setOnAddToCartCallback(this::updateCart);
            productPane.getChildren().add(card);
        }
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
        if (fxmlResource.toLowerCase().contains("card"))      stage.setTitle("Carte Salvate");
        else if (fxmlResource.toLowerCase().contains("profile"))  stage.setTitle("Dettagli Profilo");
        else if (fxmlResource.toLowerCase().contains("purchase")) stage.setTitle("Storico Acquisti");
        else stage.setTitle("E-Shop");
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
