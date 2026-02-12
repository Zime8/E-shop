package org.example.controllers.app;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.example.dao.ProductDaos;
import org.example.dao.UserDAO;
import org.example.dao.api.ProductDao;
import org.example.models.CartItem;
import org.example.models.Product;
import org.example.util.Session;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddToWishlist {

    private Consumer<List<Product>> showItemsCallback;
    private Consumer<String> showAlertCallback;
    private Consumer<Product> notifyCartCallback;

    private static final Logger logger = Logger.getLogger(AddToWishlist.class.getName());
    private final UserDAO userDao;
    private final ProductDao productDao;

    public AddToWishlist() {
        this.userDao = new UserDAO();
        this.productDao = ProductDaos.create();
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

    public boolean existsWish(String user, long productId, int shopId) {
        return productDao.existsWish(user, productId, shopId);
    }
    public boolean existsWish(String user, long productId, int shopId, String size) {
        return productDao.existsWish(user, productId, shopId, size);
    }
    public void addToWishList(String user, long productId, int shopId, String size) {
        userDao.addInWishList(user, productId, shopId, size);
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

    public Image loadProductImage(Product p) {
        try {
            return p.getImage();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore caricamento immagine: ", e);
            return null;
        }
    }
}
