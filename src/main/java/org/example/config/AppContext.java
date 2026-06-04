package org.example.config;

import org.example.control.*;
import org.example.control.dependencies.BuyProductDependencies;
import org.example.control.services.*;
import org.example.control.session.CartSession;
import org.example.control.session.SessionCartSession;
import org.example.control.session.UserContext;
import org.example.dao.*;
import org.example.dao.db.*;
import org.example.dao.ProductRepository;
import org.example.dao.demo.*;
import org.example.dao.gateway.FakePaymentGateway;
import org.example.dao.gateway.PaymentGateway;
import org.example.util.Navigator;
import org.example.util.Session;

public class AppContext {

    private final Navigator navigator;
    private final FxControllerFactory controllerFactory;

    private final BuyProductController buyProductController;
    private final WishlistAppController wishlistAppController;
    private final SellerHomeAppController sellerHomeAppController;
    private final SellerOrdersController sellerOrdersController;
    private final SellerProductsController sellerProductsController;
    private final ReviewAppController reviewAppController;
    private final Withdraw withdrawController;

    public AppContext(Session session, UserContext userContext, Navigator navigator) {
        this.navigator = navigator;
        this.controllerFactory = new FxControllerFactory(this);

        PaymentGateway paymentGateway = new FakePaymentGateway(1000, 0.1);

        CartSession cartSession = new SessionCartSession(session);
        ProductRepository productDao = userContext.isDemo()
                ? new DemoProductDAO()
                : ProductDaos.create();
        ShopRepository shopRepository = new DbShopDAO();
        UserRepository userRepository = userContext.isDemo()
                ? new DemoUserDAO()
                : new DbUserDAO();
        WishlistRepository wishlistRepository = userContext.isDemo()
                ? new DemoWishlistDAO()
                : new DbWishlistDAO();
        ReviewRepository reviewRepository = userContext.isDemo()
                ? new DemoReviewDAO()
                : new DbReviewDAO();
        OrderRepository orderRepository = userContext.isDemo()
                ? new DemoOrderDAO()
                : new DbOrderDAO();
        SavedCardsRepository savedCardsRepository = userContext.isDemo()
                ? new DemoSavedCardsDAO()
                : new DbSavedCardsDAO();

        HomeService homeService = new HomeService(productDao);
        CartService cartService = new CartService(cartSession, productDao);
        CardsService cardsService = new CardsService(savedCardsRepository);
        PurchaseHistoryService purchaseHistoryService = new PurchaseHistoryService(orderRepository, AppExecutors.IO);
        ProductDetailService productDetailService = new ProductDetailService(productDao, shopRepository);
        PaymentSelectionService paymentSelectionService = new PaymentSelectionService(paymentGateway, cardsService, orderRepository);
        SavedCardsService savedCardsService = new SavedCardsService(savedCardsRepository, userContext);
        ModifyProfile modifyProfileController = new ModifyProfile(userRepository, wishlistRepository, savedCardsService, userContext);
        CheckOrders checkOrdersController = new CheckOrders(purchaseHistoryService, userContext);

        this.wishlistAppController = new WishlistAppController(
                wishlistRepository,
                productDao,
                userContext,
                cartService
        );

        BuyProductDependencies buyProductDependencies = new BuyProductDependencies(
                homeService,
                cartService,
                productDetailService,
                paymentSelectionService,
                userContext,
                modifyProfileController,
                checkOrdersController,
                wishlistAppController
        );

        this.buyProductController = new BuyProductController(buyProductDependencies);

        this.sellerHomeAppController = new SellerHomeAppController(shopRepository, userContext);
        this.sellerOrdersController = new SellerOrdersController();
        this.sellerProductsController = new SellerProductsController();

        this.reviewAppController = new ReviewAppController(
                reviewRepository,
                userRepository,
                userContext
        );

        this.withdrawController = new Withdraw(shopRepository, paymentSelectionService);
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public FxControllerFactory getControllerFactory() {
        return controllerFactory;
    }

    public BuyProductController getBuyProductController() {
        return buyProductController;
    }

    public WishlistAppController getWishlistAppController() {
        return wishlistAppController;
    }

    public SellerHomeAppController getSellerHomeAppController() {
        return sellerHomeAppController;
    }

    public SellerOrdersController getSellerOrdersController() {
        return sellerOrdersController;
    }

    public SellerProductsController getSellerProductsController() {
        return sellerProductsController;
    }

    public ReviewAppController getReviewAppController() {
        return reviewAppController;
    }

    public Withdraw getWithdrawController() {
        return withdrawController;
    }
}