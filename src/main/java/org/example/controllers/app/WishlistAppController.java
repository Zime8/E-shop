package org.example.controllers.app;

import javafx.application.Platform;
import org.example.dao.UserDAO;
import org.example.models.CartItem;
import org.example.models.Product;
import org.example.util.Session;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WishlistAppController {

    private Consumer<List<Product>> showItemsCallback;
    private Consumer<String> showAlertCallback;
    private Consumer<Product> notifyCartCallback;

    private static final Logger logger = Logger.getLogger(WishlistAppController.class.getName());
    private final UserDAO userDao;

    public WishlistAppController() {
        this.userDao = new UserDAO();
    }

    public void init(Consumer<List<Product>> showItems,
                     Consumer<String> showAlert,
                     Consumer<Product> notifyCart) {
        this.showItemsCallback = showItems;
        this.showAlertCallback = showAlert;
        this.notifyCartCallback = notifyCart;
        loadItems();
    }

    public void loadItems() {
        try {
            List<Product> products = userDao.getFavorites(Session.getUser());
            showItemsCallback.accept(products);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento wishlist", e);
            showItemsCallback.accept(List.of());
        }
    }

    public void removeFromWishlist(Product p) {
        try {
            userDao.removeInWishlist(Session.getUser(), p.getProductId(), p.getIdShop(), p.getSize());
            loadItems();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, String.format(
                    "Errore rimuovendo wishlist (user=%s, productId=%d, shopId=%d, size=%s)",
                    Session.getUser(), p.getProductId(), p.getIdShop(), p.getSize()), ex);
            showAlertCallback.accept("Errore nella rimozione del prodotto dalla wishlist");
        }
    }

    public void addToCart(Product p) {
        CartItem item = new CartItem(
                p.getProductId(), p.getIdShop(), 1, p.getPrice(),
                p.getName(), p.getImageData(), p.getSize()
        );
        Session.addToCart(item);
        notifyCartCallback.accept(p);
        Platform.runLater(this::loadItems);
    }


    public void clearWishlist() {
        try {
            userDao.clearWishlist(Session.getUser());
            loadItems();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore nello svuotamento wishlist", e);
            showAlertCallback.accept("Errore nello svuotamento della wishlist");
        }
    }

    public javafx.scene.image.Image loadProductImage(Product p) {
        try {
            return p.getImage();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento immagine: ", e);
            return null;
        }
    }
}
