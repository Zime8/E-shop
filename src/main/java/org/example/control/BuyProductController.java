package org.example.control;

import org.example.control.services.*;
import org.example.control.session.UserContext;
import org.example.models.dto.*;
import org.example.models.entity.CartItem;
import org.example.models.entity.Product;

import java.math.BigDecimal;
import java.util.*;

public class BuyProductController {
    private final HomeService homeService;
    private final CartService cartService;
    private final ProductDetailService productDetailService;
    private final PaymentSelectionService paymentService;
    private final UserContext userContext;
    private final ModifyProfile modifyProfileController;
    private final CheckOrders checkOrdersController;
    private final WishlistAppController wishlistAppController;

    public BuyProductController(HomeService homeService,
                                CartService cartService,
                                ProductDetailService productDetailService,
                                PaymentSelectionService paymentService,
                                UserContext userContext,
                                ModifyProfile modifyProfileController,
                                CheckOrders checkOrdersController,
                                WishlistAppController wishlistAppController){
        this.homeService = homeService;
        this.cartService = cartService;
        this.productDetailService = productDetailService;
        this.paymentService = paymentService;
        this.userContext = userContext;
        this.modifyProfileController = modifyProfileController;
        this.checkOrdersController = checkOrdersController;
        this.wishlistAppController = wishlistAppController;
    }

    // Schermata home: ricerca, filtri e setup product card
    public List<ProductDto> searchByName(String query) {
        return homeService.searchByName(query).stream().map(this::toListDto).toList();
    }
    public List<ProductDto> findLatest(int limit) {
        return homeService.findLatest(limit).stream().map(this::toListDto).toList();
    }
    public List<ProductDto> searchByFilters(FilterCriteria criteria) {
        return homeService.searchByFilters(criteria).stream().map(this::toListDto).toList();
    }
    public FilterCriteria createFilterCriteria(String sport, String brand, String shop, String category, double minPrice, double maxPrice) {
        return new FilterCriteria(sport, brand, shop, category, minPrice, maxPrice);
    }
    public FilterCriteria resetFilters() {
        return FilterCriteria.defaults();
    }

    public String getCurrentUsername() {
        return userContext.getCurrentUsername();
    }

    public ModifyProfile getModifyProfileController() {
        return modifyProfileController;
    }

    public CheckOrders getCheckOrdersController() {
        return checkOrdersController;
    }

    public WishlistAppController getWishlistController() {
        return wishlistAppController;
    }

    public void logout() {
        userContext.logout();
    }

    private ProductDto toListDto(Product p) {
        return new ProductDto(p.productId(), p.idShop(), p.name(), p.nameShop(), p.sport(),
                p.price().doubleValue(), p.imageData(), 0, null);
    }

    // Schermata dettaglio prodotto: dettagli prodotto, wishlist, stock, cart add
    public ShopDto getShopInfo(int shopId) {
        return productDetailService.getShopInfo(shopId);
    }
    public List<String> getAvailableSizes(long productId, int shopId) {
        return productDetailService.getAvailableSizes(productId, shopId);
    }
    public double getPriceFor(long productId, int shopId, String size) {
        return productDetailService.getPriceFor(productId, shopId, size);
    }
    public Integer getStockFor(long productId, int shopId, String size) {
        return productDetailService.getStockFor(productId, shopId, size);
    }

    public void addToCart(AddToCartRequest request) {
        cartService.addToCart(request);
    }

    public boolean isValidQuantity(int quantity, int maxStock) {
        return productDetailService.isValidQuantity(quantity, maxStock);
    }

    // SChermata gestione carrello

    public int getCartCount() {
        return cartService.getCartCount();
    }

    public CheckoutData buildCheckoutData() {
        return cartService.buildCheckoutData();
    }

    public void changeQuantity(long productId, int shopId, String size, int delta) {
        cartService.changeQuantity(productId, shopId, size, delta);
    }

    public void removeLine(long productId, int shopId, String size) {
        cartService.removeLine(productId, shopId, size);
    }
    public void clearCart() {
        cartService.clearCart();
    }

    public DisplayData buildDisplayData() {
        return cartService.buildDisplayData();
    }

    public List<CartRowData> loadCartRows() {
        return cartService.loadCartRows();
    }

    // Schermata del pagamento

    public CardsService.AddCardResult addInlineCard(InlineCardData data) {
        return paymentService.addInlineCard(userContext.getCurrentUserId(), data);
    }

    public List<Card> loadSavedCardsForCurrentUser() {
        return paymentService.loadSavedCards(userContext.getCurrentUserId());
    }

    public CheckoutResult confirmPayment(CheckoutRequest request) {
        List<CartItem> items = cartService.getCartItems();
        BigDecimal total = cartService.buildCheckoutData().total();

        return paymentService.confirmPayment(
                request.card(),
                request.cvv(),
                request.address(),
                items,
                total,
                userContext.getCurrentUserId()
        );
    }

}
